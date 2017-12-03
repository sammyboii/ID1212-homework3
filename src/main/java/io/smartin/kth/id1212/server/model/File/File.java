package io.smartin.kth.id1212.server.model.File;

import io.smartin.kth.id1212.server.model.User.User;
import io.smartin.kth.id1212.shared.interfaces.CatalogFile;
import io.smartin.kth.id1212.shared.DTOs.Metadata;

/*
* A file has the following attributes: name;
size; owner; public/private access permissions that indicates whether it’s a public
or private file; write and read permissions if the file is public. I
* */
public class File implements CatalogFile {
    private final String name;
    private final long size;
    private boolean notifyOwner = false;
    private final FilePermissions permissions;
    private String url;
    private boolean completed = false;
    public static final String storagePath = "/Users/smartin/Desktop/uploads/";

    public File (String name, long size, User owner, boolean isPublic, boolean isReadable, boolean isWritable) {
        this.name = name;
        this.size = size;
        this.permissions = new FilePermissions(owner, isPublic, isReadable, isWritable);
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getSize() {
        return size;
    }

    @Override
    public boolean isPublic() {
        return permissions.isPublic();
    }

    @Override
    public boolean isWritable() {
        return permissions.isWritable();
    }

    @Override
    public boolean isReadable() {
        return permissions.isReadable();
    }

    @Override
    public String getPath() {
        return "";
    }

    public void configureNotifications(boolean shouldNotify) {
        notifyOwner = shouldNotify;
    }

    public boolean notifiesOwner() {
        return notifyOwner;
    }

    public FilePermissions getPermissions() {
        return permissions;
    }

    public Metadata getMetadata () {
        Metadata metadata = new Metadata(name, null, size, notifyOwner, isPublic(), isReadable(), isWritable());
        metadata.setOwnerName(getPermissions().getOwner().getUsername());
        return metadata;
    }

    public static File fromMetadata (User owner, Metadata metadata) {
        return new File(metadata.getName(), metadata.getSize(), owner, metadata.isPublic(),
                metadata.isReadable(), metadata.isWritable());
    }
}
