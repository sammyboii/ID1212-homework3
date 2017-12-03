package io.smartin.kth.id1212.shared.interfaces;

import io.smartin.kth.id1212.server.exceptions.UnauthorizedWriteException;
import io.smartin.kth.id1212.shared.DTOs.Credentials;
import io.smartin.kth.id1212.shared.DTOs.Metadata;
import io.smartin.kth.id1212.shared.DTOs.TransferRequest;
import io.smartin.kth.id1212.shared.exceptions.UnauthorizedReadException;

import java.io.FileNotFoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

public interface FileCatalog extends Remote {
    String NAME_IN_REGISTRY = "FILE_CATALOG";
    void register (FileClient remoteObject, Credentials credentials) throws RemoteException;
    UUID unregister (UUID userID) throws RemoteException;
    UUID logIn (FileClient remoteObject, Credentials credentials) throws RemoteException, IllegalArgumentException;
    UUID logOut (UUID userID) throws RemoteException;
    void uploadFile (UUID userID, Metadata metadata) throws RemoteException, UnauthorizedWriteException;
    Metadata requestFile(TransferRequest socketID) throws RemoteException, UnauthorizedReadException, FileNotFoundException;
    boolean deleteFile (UUID userID, String fileName) throws RemoteException, FileNotFoundException, UnauthorizedWriteException;
    List<Metadata> listFiles (UUID userID) throws RemoteException;
    void setPermissions (UUID userID, String fileName, String permissions) throws RemoteException, FileNotFoundException, UnauthorizedWriteException;
    void subscribe (UUID userID, String fileName, boolean active) throws RemoteException, FileNotFoundException, UnauthorizedReadException;
}
