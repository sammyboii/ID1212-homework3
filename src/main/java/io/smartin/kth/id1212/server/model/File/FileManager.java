package io.smartin.kth.id1212.server.model.File;

import io.smartin.kth.id1212.server.controller.Controller;
import io.smartin.kth.id1212.server.exceptions.NoSuchTableException;
import io.smartin.kth.id1212.server.exceptions.UnauthorizedWriteException;
import io.smartin.kth.id1212.shared.DTOs.TransferRequest;
import io.smartin.kth.id1212.shared.exceptions.UnauthorizedReadException;
import io.smartin.kth.id1212.shared.exceptions.UserNotFoundException;
import io.smartin.kth.id1212.server.integration.DatabaseHandler;
import io.smartin.kth.id1212.server.model.User.User;
import io.smartin.kth.id1212.shared.DTOs.Metadata;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;

public class FileManager {
    private final DatabaseHandler db = new DatabaseHandler();
    private Controller controller;
    private FileObserver fileObserver;
    private final List<FileTransferer> fileTransferList = new ArrayList<>();

    public FileManager (Controller controller) {
        try {
            this.fileObserver = new FileObserver();
            this.controller = controller;
            db.connect("localhost");
            new TransferListener().start(this);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public FileObserver getObserver() {
        return fileObserver;
    }

    public List<Metadata> listFiles (User user) throws SQLException {
        List<File> files = db.getAllFiles();
        List<Metadata> filteredData = new ArrayList<>();
        files.forEach(file -> {
            if (file.getPermissions().isReadableBy(user) && file.isCompleted()) {
                filteredData.add(file.getMetadata());
            }
        });
        return filteredData;
    }

    public boolean prepareUpload(User user, Metadata metadata) throws SQLException, UnauthorizedWriteException {
        try {
            File file = db.getFileByName(metadata.getName());
            if (file.getPermissions().isWritableBy(user)) {
                db.updateFile(File.fromMetadata(user, metadata));
                return true;
            } else {
                throw new UnauthorizedWriteException("You are not allowed to write to this file");
            }
        } catch (FileNotFoundException e) {
            return db.storeFileMetaData(user, metadata);
        }
    }

    public Metadata prepareDownload(UUID uuid, User user, String filename) throws SQLException, FileNotFoundException, UnauthorizedReadException {
        File file = db.getFileByName(filename);
        if (!file.getPermissions().isReadableBy(user)) {
            throw new UnauthorizedReadException("You are not allowed to read this file");
        }
        return file.getMetadata();
    }

    public void abortUpload(String filename) {
        synchronized (fileTransferList) {
            FileTransferer target = null;
            for (FileTransferer fileUpload : fileTransferList) {
                if (fileUpload.getFileName().equals(filename)) {
                    target = fileUpload;
                    break;
                }
            }
            if (target != null) {
                removeUpload(target);
            }
        }
    }

    public Controller getController() {
        return controller;
    }

    public File verifyWritable(UUID userID, String filename) throws SQLException, UserNotFoundException, UnauthorizedWriteException, FileNotFoundException {
        User user = controller.getUserManager().getUser(userID);
        File file = db.getFileByName(filename);
        if (file.getPermissions().isWritableBy(user)) {
            return file;
        }
        throw new UnauthorizedWriteException("The user is not authorized to write to that filename");
    }

    public File verifyReadable(UUID userID, String filename) throws UnauthorizedWriteException, UserNotFoundException, FileNotFoundException, SQLException {
        User user = controller.getUserManager().getUser(userID);
        File file = db.getFileByName(filename);
        if (file.getPermissions().isReadableBy(user)) {
            return file;
        }
        throw new UnauthorizedWriteException("The user is not authorized to write to that filename");
    }

    private void removeUpload(FileTransferer upload) {
        synchronized (fileTransferList) {
            fileTransferList.remove(upload);
        }
    }

    public void completeUpload(FileTransferer upload, File file, UUID userID) throws SQLException {
        User user = null;
        try {
            file.setCompleted(true);
            db.updateFile(file);
            user = controller.getUserManager().getUser(userID);
            user.sendResponse("File '" + file.getName() + "' uploaded successfully");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (UserNotFoundException e) {
            System.out.println("User disconnected before response could be sent");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            if (user != null) {
                try {
                    user.sendResponse("There was an error storing your file");
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            }
        }
        removeUpload(upload);
    }

    public boolean updatePermissions(String filename, User user, String permissions) throws FileNotFoundException, SQLException, UnauthorizedWriteException {
        File file = db.getFileByName(filename);
        if (!file.getPermissions().isWritableBy(user)) {
            throw new UnauthorizedWriteException("You are not allowed to modify this file");
        }
        file.getPermissions().configurePublic(permissions.contains("p"));
        file.getPermissions().configureReadable(permissions.contains("r"));
        file.getPermissions().configureWritable(permissions.contains("w"));
        db.updateFile(file);
        fileObserver.notifyChanged(file, user);
        return true;
    }

    public boolean updateSubscription (String filename, User user, boolean preference) throws FileNotFoundException, SQLException, UnauthorizedReadException {
        File file = db.getFileByName(filename);
        if (!file.getPermissions().getOwner().getUsername().equals(user.getUsername())) {
            throw new UnauthorizedReadException("You are not allowed to get notifications about this file");
        }
        file.configureNotifications(preference);
        db.updateFile(file);
        fileObserver.notifyChanged(file, user);
        return true;
    }

    public boolean deleteFile(User user, String fileName) throws NoSuchTableException, SQLException, FileNotFoundException, UnauthorizedWriteException {
        File file = db.getFileByName(fileName);
        if (file.getPermissions().isWritableBy(user)) {
            try {
                db.deleteFrom(DatabaseHandler.Table.FILES, fileName);
                Files.delete(Paths.get(File.storagePath, file.getName()));
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new UnauthorizedWriteException("You are not allowed to erase this file");
        }
        return false;
    }

    public void completeDownload(TransferRequest socketID) {
        try {
            User user = controller.getUserManager().getUser(socketID.getUuid());
            user.sendResponse("Transfer of '" + socketID.getFilename() + "' complete");
        } catch (RemoteException | UserNotFoundException e) {
            e.printStackTrace();
        }
    }

    private class TransferListener implements Runnable {
        FileManager fm;
        static final int port = 1337;

        void start(FileManager fm) {
            this.fm = fm;
            new Thread(this).start();
        }

        @Override
        public void run() {
            try {
                ServerSocketChannel listeningSocket = initChannel();
                while (true) {
                    SocketChannel clientSocket = listeningSocket.accept();
                    startTransfer(clientSocket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private ServerSocketChannel initChannel () throws IOException {
            InetSocketAddress address = new InetSocketAddress(port);
            ServerSocketChannel listeningSocket = ServerSocketChannel.open();
            listeningSocket.socket().bind(address);
            return listeningSocket;
        }


        void startTransfer (SocketChannel clientSocket) {
            FileTransferer fileTransferer = new FileTransferer(clientSocket, fm);
            new Thread(fileTransferer).start();
            synchronized (fileTransferList) {
                fileTransferList.add(fileTransferer);
            }
        }
    }

    public class FileObserver {

        public void notifyDownloaded(File file, TransferRequest transferRequest) {
            try {
                User owner = file.getPermissions().getOwner();
                List <User> connectedOwner = controller.getUserManager().findUserSessions(owner.getUsername());
                User downloader = controller.getUserManager().getUser(transferRequest.getUuid());
                if (ownerIsResponsible(owner, downloader)) {
                    return; // ignore if owner
                }
                String notice = "<!> Your file '" + file.getName() + "' was just downloaded by " + downloader.getUsername();
                for (User connected : connectedOwner) {
                    connected.sendResponse(notice);
                }
            } catch (UserNotFoundException | RemoteException e) {
                e.printStackTrace();
            }
        }

        private boolean ownerIsResponsible(User owner, User editor) {
            return owner.getUsername().equals(editor.getUsername());
        }

        void notifyChanged(File file, User editor) {
            try {
                User owner = file.getPermissions().getOwner();
                if (ownerIsResponsible(owner, editor)) {
                    return; // ignore if owner
                }
                String notice = "<!> Your file '" + file.getName() + "' was just edited by " + editor.getUsername();
                owner.sendResponse(notice);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }
}
