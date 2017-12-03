package io.smartin.kth.id1212.server.model.User;

import io.smartin.kth.id1212.shared.interfaces.FileClient;

import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;

public class User {
    private final String username;
    private final String password;
    private FileClient remoteNode;
    private SocketChannel socketChannel;

    public void sendResponse(String message) throws RemoteException {
        remoteNode.receiveResponse(message);
    }

    public void forceLogOut() throws RemoteException {
        remoteNode.forceLogOut();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setSocketChannel (SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public FileClient getRemoteNode() {
        return remoteNode;
    }

    public User (String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void setRemoteNode (FileClient remoteNode) {
        this.remoteNode = remoteNode;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }
}
