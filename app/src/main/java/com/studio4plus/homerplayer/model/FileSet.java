package com.studio4plus.homerplayer.model;

import com.google.common.base.Preconditions;

import java.io.File;

public class FileSet {

    public final String id;
    public final String directoryName;
    public final File[] files;
    public final boolean isDemoSample;

    public FileSet(String id, File absolutePath, File[] files, boolean isDemoSample) {
        Preconditions.checkArgument(absolutePath.isDirectory());
        this.id = id;
        this.directoryName = absolutePath.getName();
        this.files = files;
        this.isDemoSample = isDemoSample;
    }

    /**
     * Compares only the id field.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileSet fileSet = (FileSet) o;

        return id.equals(fileSet.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
