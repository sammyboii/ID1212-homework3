package io.smartin.kth.id1212.client.startup;

import io.smartin.kth.id1212.client.view.Client;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Main {
    private static final String host = "localhost";

    public static void main(String[] args) {
        try {
            new Client(host).start();
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
