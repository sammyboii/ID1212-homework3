package io.smartin.kth.id1212.client.view;

import io.smartin.kth.id1212.client.exceptions.ErroneousInputException;
import io.smartin.kth.id1212.client.exceptions.NoArgumentException;
import io.smartin.kth.id1212.client.exceptions.UnknownCommandException;
import io.smartin.kth.id1212.server.exceptions.UnauthorizedWriteException;
import io.smartin.kth.id1212.shared.DTOs.Credentials;
import io.smartin.kth.id1212.shared.DTOs.Metadata;
import io.smartin.kth.id1212.shared.DTOs.TransferRequest;
import io.smartin.kth.id1212.shared.exceptions.UnauthorizedReadException;
import io.smartin.kth.id1212.shared.interfaces.FileCatalog;
import io.smartin.kth.id1212.shared.interfaces.FileClient;
import io.smartin.kth.id1212.shared.tools.TransferHandler;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Client implements Runnable {
    private boolean running = false;
    private final static String inputSign = "->";
    private FileCatalog catalog;
    private final FileClient remoteObject;
    private UUID userID = null;
    private final String host;

    public Client(String host) throws RemoteException, MalformedURLException, NotBoundException {
        this.host = host;
        remoteObject = new ResponseHandler();
        catalog = findCatalog(host);
    }

    public void start() {
        new Thread(this).start();
    }

    private void showInputSign() {
        System.out.print(inputSign + " ");
    }

    public void run() {
        running = true;
        Scanner scanner = new Scanner(System.in);
        showInputSign();
        while (running) {
            String command = scanner.nextLine();
            handleCommand(command);
        }
    }

    private void handleCommand(String cmd) {
        try {
            Command command = Command.parse(cmd);
            String username = null;
            String password = null;
            Credentials credentials = null;
            String response = null;
            switch (command.getType()) {
                case QUIT:
                    running = false;
                    if (isLoggedIn()) {
                        catalog.logOut(userID);
                    }
                    UnicastRemoteObject.unexportObject(remoteObject, false);
                    quit();
                    break;
                case REGISTER:
                    username = command.getArg(0);
                    password = command.getArg(1);
                    credentials = new Credentials(username, password);
                    catalog.register(remoteObject, credentials);
                    break;
                case UNREGISTER:
                    if (!isLoggedIn()) {
                        out("You need to be logged in to unregister your account");
                        break;
                    }
                    userID = catalog.unregister(userID);
                    break;
                case LOGIN:
                    if (isLoggedIn()) {
                        out("You need to log out before you log in");
                        break;
                    }
                    username = command.getArg(0);
                    password = command.getArg(1);
                    credentials = new Credentials(username, password);
                    userID = catalog.logIn(remoteObject, credentials);
                    break;
                case LOGOUT:
                    if (!isLoggedIn()) {
                        out("You need to log in before you log out");
                        break;
                    }
                    userID = catalog.logOut(userID);
                    break;
                case LIST:
                    if (!isLoggedIn()) {
                        out("You need to log in before you can view files");
                        break;
                    }
                    List<Metadata> files = catalog.listFiles(userID);
                    renderList(files);
                    break;
                case UPLOAD:
                    if (!isLoggedIn()) {
                        out("You need to log in before you upload");
                        break;
                    }
                    String uniqueName = command.getArg(0);
                    String filePath = command.getArg(1);
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(filePath);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    String perms = command.getArg(2);

                    long size = 0;
                    try {
                        size = fis.getChannel().size();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    boolean isPublic = perms.contains("p");
                    boolean isReadable = perms.contains("r");
                    boolean isWritable = perms.contains("w");
                    boolean shouldNotify = perms.contains("n");

                    Metadata metadata = new Metadata(uniqueName, filePath, size, shouldNotify, isPublic, isReadable, isWritable);
                    try {
                        catalog.uploadFile(userID, metadata);
                        new Thread(new FileTransfer(metadata, true)).start();
                    } catch (UnauthorizedWriteException e) {
                        out(e.getMessage());
                    }
                    break;
                case DOWNLOAD:
                    if (!isLoggedIn()) {
                        out("You need to log in before you can download");
                        break;
                    }
                    String fileToDownload = command.getArg(0);
                    try {
                        TransferRequest transferReq = new TransferRequest(userID, fileToDownload, false);
                        Metadata potentialFile = catalog.requestFile(transferReq);
                        new Thread(new FileTransfer(potentialFile, false)).start();
                    } catch (UnauthorizedReadException | FileNotFoundException e) {
                        out(e.getMessage());
                    }
                    break;
                case PERMISSIONS:
                    String permissions = command.getArg(0);
                    String fileToUpdate = command.getArg(1);
                    try {
                        catalog.setPermissions(userID, fileToUpdate, permissions);
                        out("Permissions for '" + fileToUpdate + "' updated");
                    } catch (FileNotFoundException | UnauthorizedWriteException e) {
                        out(e.getMessage());
                    }
                    break;
                case SUBSCRIBE:
                    String fileToNotifyAbout = command.getArg(0);
                    handleSubscription(fileToNotifyAbout, true);
                    break;
                case UNSUBSCRIBE:
                    String fileToStopNotifyingAbout = command.getArg(0);
                    handleSubscription(fileToStopNotifyingAbout, false);
                    break;
                case DELETE:
                    String fileToDelete = command.getArg(0);
                    try {
                        catalog.deleteFile(userID, fileToDelete);
                        out("File '"+ fileToDelete +"' deleted");
                    } catch (FileNotFoundException | UnauthorizedWriteException e) {
                        out(e.getMessage());
                    }
                    break;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException | ErroneousInputException | NoArgumentException e) {
            out(e.getMessage());
        } catch (UnknownCommandException e) {
            System.out.println(e.getMessage());
            System.out.println("Available commands:");
            for (Command.CommandType commandType : Command.CommandType.values()) {
                System.out.println("- " + commandType.toString().toLowerCase());
            }
            showInputSign();
        }
    }

    private void handleSubscription(String fileToNotifyAbout, boolean active) throws RemoteException {
        try {
            String change = active ? "activated" : "removed";
            catalog.subscribe(userID, fileToNotifyAbout, active);
            out("Subscription for '" + fileToNotifyAbout + "' " + change);
        } catch (FileNotFoundException | UnauthorizedReadException e) {
            out(e.getMessage());
        }
    }

    private void renderList(List<Metadata> files) {
        files.forEach(file -> {
            StringJoiner j = new StringJoiner("\t \t");
            String perms = formatPermissions(file.isPublic(), file.isReadable(), file.isWritable());
            j.add("filename: " + file.getName());
            j.add("size: " + file.getSize() + " b");
            j.add("owner: " + file.getOwnerName());
            j.add("permissions: " + perms);
            System.out.println("* " + j.toString());
        });
        showInputSign();
    }

    private String formatPermissions (boolean p, boolean r, boolean w) {
        String sb = (p ? "p" : "-") +
                (r ? "r" : "-") +
                (w ? "w" : "-");
        return sb;
    }

    private boolean isLoggedIn() {
        return userID != null;
    }

    private void quit() {
        running = false;
    }

    private static FileCatalog findCatalog(String host) throws NotBoundException, MalformedURLException, RemoteException {
        return (FileCatalog) Naming.lookup("//" + host + "/" + FileCatalog.NAME_IN_REGISTRY);
    }

    private class ResponseHandler extends UnicastRemoteObject implements FileClient {
        ResponseHandler() throws RemoteException {
        }

        public void forceLogOut() throws RemoteException {
            userID = null;
        }

        public void receiveResponse(String response) {
            out(response);
        }
    }

    private void out(String message) {
        System.out.println(message);
        showInputSign();
    }

    private class FileTransfer implements Runnable {
        final Metadata metadata;
        final boolean isUpload;
        final String downloadsFolder = "/Users/smartin/Desktop/socketDL/";

        FileTransfer(Metadata metadata, boolean isUpload) {
            this.metadata = metadata;
            this.isUpload = isUpload;
        }

        private Path determineFinalPath() {
            File f = new File(downloadsFolder + metadata.getName());
            int i = 1;
            String filename = metadata.getName();
            while (f.exists() && !f.isDirectory()) {
                filename = metadata.getName() + "("+ i +")";
                f = new File(downloadsFolder + filename);
            }
            return Paths.get(downloadsFolder, filename);
        }

        @Override
        public void run() {
            try {
                SocketChannel socketChannel = TransferHandler.getValidSocketChannel(host,
                        1337, new TransferRequest(userID, metadata.getName(), isUpload));
                if (isUpload) {
                    TransferHandler.sendFile(Paths.get(metadata.getPath()), socketChannel);
                } else {
                    Path finalPath = determineFinalPath();
                    TransferHandler.receiveFile(metadata.getSize(), finalPath, socketChannel);
                }
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
