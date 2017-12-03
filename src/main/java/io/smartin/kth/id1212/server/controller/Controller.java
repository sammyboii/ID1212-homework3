package io.smartin.kth.id1212.server.controller;

import io.smartin.kth.id1212.client.exceptions.ErroneousInputException;
import io.smartin.kth.id1212.server.exceptions.NoSuchTableException;
import io.smartin.kth.id1212.server.exceptions.UnauthorizedWriteException;
import io.smartin.kth.id1212.server.exceptions.UserAlreadyExistsException;
import io.smartin.kth.id1212.shared.DTOs.Credentials;
import io.smartin.kth.id1212.shared.DTOs.Metadata;
import io.smartin.kth.id1212.shared.DTOs.TransferRequest;
import io.smartin.kth.id1212.shared.exceptions.UnauthorizedReadException;
import io.smartin.kth.id1212.shared.exceptions.UserNotFoundException;
import io.smartin.kth.id1212.server.model.File.FileManager;
import io.smartin.kth.id1212.server.model.User.User;
import io.smartin.kth.id1212.server.model.User.UserManager;
import io.smartin.kth.id1212.shared.interfaces.FileCatalog;
import io.smartin.kth.id1212.shared.interfaces.FileClient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Controller extends UnicastRemoteObject implements FileCatalog {
    private final UserManager userManager = new UserManager();
    private final FileManager fileManager = new FileManager(this);

    public Controller() throws RemoteException {
    }

    @Override
    public void register(FileClient remoteObject, Credentials credentials) throws RemoteException {
        try {
            userManager.createUser(credentials);
            remoteObject.receiveResponse("User '" + credentials.getUsername() + "' created");
        } catch (UserAlreadyExistsException e) {
            remoteObject.receiveResponse(e.getMessage());
        } catch (NoSuchTableException | SQLException e) {
            remoteObject.receiveResponse("Could not register user (internal error)");
        }
    }

    public UserManager getUserManager() {
        return userManager;
    }

    @Override
    public UUID unregister(UUID userID) throws RemoteException {
        try {
            userManager.deleteUser(userID);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userID;
    }

    @Override
    public UUID logIn(FileClient remoteNode, Credentials credentials) throws RemoteException, IllegalArgumentException {
        try {
            User user = userManager.authenticateUser(credentials);
            user.setRemoteNode(remoteNode);
            UUID userID = userManager.logIn(user);
            user.sendResponse("Logged in (" + user.getUsername() + ")");
            return userID;
        } catch (UserNotFoundException | ErroneousInputException e) {
            remoteNode.receiveResponse("Incorrect username or password");
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            remoteNode.receiveResponse("Internal error. Try again later.");
        }
        return null;
    }

    @Override
    public UUID logOut(UUID userID) throws RemoteException {
        User loggedOutUser = userManager.logOut(userID);
        loggedOutUser.getRemoteNode().receiveResponse("Logged out '" + loggedOutUser.getUsername() + "'");
        return null;
    }

    @Override
    public List<Metadata> listFiles(UUID userID) throws RemoteException {
        User user = null;
        try {
            user = userManager.getUser(userID);
            return fileManager.listFiles(user);
        } catch (SQLException | UserNotFoundException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public boolean uploadFile (UUID userID, Metadata metadata) throws RemoteException, UnauthorizedWriteException {
        User user = null;
        try {
            user = userManager.getUser(userID);
            return fileManager.prepareUpload(user, metadata);
        } catch (SQLIntegrityConstraintViolationException e) {
            if (user != null) {
                user.sendResponse("You are not allowed to upload to this filename");
            } else {
                e.printStackTrace();
            }
        } catch (UserNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Metadata requestFile (TransferRequest socketID) throws RemoteException, UnauthorizedReadException, FileNotFoundException {
        User user = null;
        try {
            user = userManager.getUser(socketID.getUuid());
            return fileManager.prepareDownload(socketID.getUuid(), user, socketID.getFilename());
        } catch (UserNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean setPermissions (UUID userID, String fileName, String permissions) throws FileNotFoundException, UnauthorizedWriteException {
        try {
            User user = userManager.getUser(userID);
            return fileManager.updatePermissions(fileName, user, permissions);
        } catch (UserNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean subscribe(UUID userID, String fileName, boolean active) throws RemoteException, FileNotFoundException, UnauthorizedReadException {
        try {
            User user = userManager.getUser(userID);
            return fileManager.updateSubscription(fileName, user, active);
        } catch (UserNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deleteFile(UUID userID, String fileName) throws FileNotFoundException, UnauthorizedWriteException {
        try {
            User user = userManager.getUser(userID);
            return fileManager.deleteFile(user, fileName);
        } catch (SQLException | UserNotFoundException | NoSuchTableException e) {
            e.printStackTrace();
        }
        return false;
    }
}
