package io.smartin.kth.id1212.shared.DTOs;

import java.io.Serializable;
import java.util.UUID;

public class TransferRequest implements Serializable {
    private final UUID uuid;
    private final String filename;
    private final boolean isUpload;

    public TransferRequest(UUID uuid, String filename, boolean isUpload) {
        this.uuid = uuid;
        this.filename = filename;
        this.isUpload = isUpload;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getFilename() {
        return filename;
    }

    public boolean isUpload (){
        return isUpload;
    }
}
