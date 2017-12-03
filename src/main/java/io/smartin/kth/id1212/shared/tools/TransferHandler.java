package io.smartin.kth.id1212.shared.tools;

import io.smartin.kth.id1212.shared.DTOs.TransferRequest;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

public class TransferHandler {
    private static final int bufferSize = 1024;

    public static void receiveFile(long size, Path destination, SocketChannel channel) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(destination, EnumSet.of(StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {

            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            long numBytesReceived = 0;

            while (numBytesReceived < size) {
                long read = channel.read(buffer);
                if (read <= 0) break;
                numBytesReceived += read;
                buffer.flip();
                fileChannel.write(buffer);
                buffer.clear();
            }
        }
    }

    public static void sendFile(Path source, SocketChannel channel) throws IOException {
        try (FileChannel fileChannel = FileChannel.open(source)) {
            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            while (fileChannel.read(buffer) > 0) {
                buffer.flip();
                channel.write(buffer);
                buffer.clear();
            }
        }
    }

    public static SocketChannel getValidSocketChannel(String host, int port, TransferRequest transferRequest) throws IOException {
        InetSocketAddress address = new InetSocketAddress(host, port);
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(address);
        ObjectOutputStream out = new ObjectOutputStream(socketChannel.socket().getOutputStream());
        out.writeObject(transferRequest);
        out.flush();
        return socketChannel;
    }
}
