package io.smartin.kth.id1212.server.startup;

import io.smartin.kth.id1212.server.controller.Controller;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    public static void main(String[] args) {
        try {
            new Main().startRegistry();
            Naming.rebind(Controller.NAME_IN_REGISTRY, new Controller());
            System.out.println("Catalog is up and running...");
        } catch (MalformedURLException | RemoteException rex) {
            System.out.println("Could not start catalog");
        }
    }

    private void startRegistry() throws RemoteException {
        try {
            LocateRegistry.getRegistry().list();
        } catch (RemoteException rex) {
            LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        }
    }
}
