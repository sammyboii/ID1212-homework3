package io.smartin.kth.id1212.shared.DTOs;

import java.io.Serializable;

public class Metadata implements Serializable {
    private final String name;
    private final String path;
    private final long size;
    private final boolean shouldNotify;
    private final boolean isPublic;
    private final boolean isReadable;
    private final boolean isWritable;
    private String ownerName = "Anonymous";

    public Metadata(String name, String path, long size, boolean shouldNotify, boolean isPublic, boolean isReadable, boolean isWritable) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.shouldNotify = shouldNotify;
        this.isPublic = isPublic;
        this.isReadable = isReadable;
        this.isWritable = isWritable;
    }

    public void setOwnerName(String name) {
        ownerName = name;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public long getSize()  {
        return size;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isWritable() {
        return isWritable;
    }

    public boolean isReadable() {
        return isReadable;
    }

    public boolean shouldNotify() {
        return shouldNotify;
    }
}
