package io.smartin.kth.id1212.shared.interfaces;

import java.io.Serializable;

public interface CatalogFile extends Serializable {
    String getName();

    long getSize();

    boolean isPublic();

    boolean isWritable();

    boolean isReadable();

    String getPath();
}
