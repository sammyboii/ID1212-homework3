package io.smartin.kth.id1212.server.model.File;

import io.smartin.kth.id1212.server.model.User.User;

public class FilePermissions {
    private final User owner;
    private boolean isPublic;
    private boolean isReadable;
    private boolean isWritable;

    FilePermissions(User owner, boolean isPublic, boolean isReadable, boolean isWritable) {
        this.owner = owner;
        this.isPublic = isPublic;
        this.isReadable = isReadable;
        this.isWritable = isWritable;
    }

    public User getOwner() {
        return owner;
    }

    public void configureWritable (boolean isWritable) {
        this.isWritable = isWritable;
    }

    public void configureReadable (boolean isReadable) {
        this.isReadable = isReadable;
    }

    public void configurePublic (boolean isPublic) {
        this.isPublic = isPublic;
    }

    public boolean isReadable () {
        return isReadable;
    }

    public boolean isWritable () {
        return isWritable;
    }

    public boolean isPublic () {
        return isPublic;
    }

    public boolean isReadableBy (User user) {
        return (isOwner(user) || (isReadable && canAccess(user)));
    }

    public boolean isWritableBy (User user) {
        return (isOwner(user) || (isWritable && canAccess(user)));
    }

    private boolean canAccess (User user) {
        return (isPublic || isOwner(user));
    }

    private boolean isOwner (User user) {
        return (user.getUsername().equals(owner.getUsername()));
    }
}
