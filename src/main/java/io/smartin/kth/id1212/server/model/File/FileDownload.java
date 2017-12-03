package io.smartin.kth.id1212.server.model.File;

import io.smartin.kth.id1212.shared.DTOs.TransferRequest;
import io.smartin.kth.id1212.shared.tools.TransferHandler;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

public class FileDownload implements Runnable {
    private final FileManager fileManager;
    private final TransferRequest socketID;
    private final SocketChannel channel;
    private final Path readPath;

    public FileDownload(FileManager fileManager, TransferRequest socketID, SocketChannel channel, Path readPath) {
        this.fileManager = fileManager;
        this.socketID = socketID;
        this.channel = channel;
        this.readPath = readPath;
    }

    @Override
    public void run() {
        try {
            TransferHandler.sendFile(readPath, channel);
            fileManager.completeDownload(socketID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
