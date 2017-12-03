package io.smartin.kth.id1212.server.model.File;

import io.smartin.kth.id1212.server.exceptions.UnauthorizedWriteException;
import io.smartin.kth.id1212.shared.DTOs.TransferRequest;
import io.smartin.kth.id1212.shared.tools.TransferHandler;
import io.smartin.kth.id1212.shared.exceptions.UserNotFoundException;
import io.smartin.kth.id1212.server.model.User.User;

import java.io.*;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.sql.SQLException;

class FileTransferer implements Runnable {
    private final FileManager fileManager;
    private final SocketChannel clientSocketChannel;
    private TransferRequest transferRequest;
    private File file;
    private Path outPath;
    private boolean isUpload = true;

    public FileTransferer(SocketChannel clientSocketChannel, FileManager fileManager) {
        this.clientSocketChannel = clientSocketChannel;
        this.fileManager = fileManager;
    }

    private void setFile(File file) {
        this.file = file;
        this.outPath = Paths.get("/Users/smartin/Desktop/uploads/" + file.getName());
    }

    private void disconnect() {
        try {
            clientSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getFileName () {
        return file.getName();
    }

    @Override
    public void run() {
        try {
            ObjectInputStream in = new ObjectInputStream(clientSocketChannel.socket().getInputStream());
            File file = validateUserFile(in);
            setFile(file);
            if (isUpload) {
                startUpload(file);
            }
            else {
                startDownload(Paths.get(File.storagePath, file.getName()), clientSocketChannel);
                if (file.notifiesOwner()) {
                    notifyDownloaded(file);
                }
            }
        } catch (IOException | SQLException | UserNotFoundException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (UnauthorizedWriteException e) {
            notifyUnauthorized();
        }
    }

    private void notifyDownloaded (File file) {
        fileManager.getObserver().notifyDownloaded(file, transferRequest);
    }

    private void startDownload(Path readPath, SocketChannel channel) throws IOException {
        TransferHandler.sendFile(readPath, channel);
        fileManager.completeDownload(transferRequest);
    }

    private void startUpload(File file) throws IOException, SQLException {
        TransferHandler.receiveFile(file.getSize(), outPath, clientSocketChannel);
        file.setCompleted(true);
        fileManager.completeUpload(this, file, transferRequest.getUuid());
    }

    private File validateUserFile(ObjectInputStream in) throws IOException, ClassNotFoundException, UserNotFoundException, UnauthorizedWriteException, SQLException {
        transferRequest = (TransferRequest) in.readObject();
        isUpload = transferRequest.isUpload();
        File file;
        if (isUpload) {
            file = fileManager.verifyWritable(transferRequest.getUuid(), transferRequest.getFilename());
        } else {
            file = fileManager.verifyReadable(transferRequest.getUuid(), transferRequest.getFilename());
        }
        User user = fileManager.getController().getUserManager().getUser(transferRequest.getUuid());
        user.setSocketChannel(clientSocketChannel);
        return file;
    }

    private void notifyUnauthorized() {
        try {
            User user = fileManager.getController().getUserManager().getUser(transferRequest.getUuid());
            user.sendResponse("You are not allowed to write to the filename: '" + transferRequest.getFilename() + "'");
        } catch (RemoteException | UserNotFoundException e) {
            e.printStackTrace();
        }
    }
}
