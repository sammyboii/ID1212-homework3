package io.smartin.kth.id1212.shared.interfaces;

import io.smartin.kth.id1212.shared.DTOs.Metadata;

import java.io.FileNotFoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FileClient extends Remote {
    void receiveResponse (String response) throws RemoteException;
    void forceLogOut() throws RemoteException;
}
