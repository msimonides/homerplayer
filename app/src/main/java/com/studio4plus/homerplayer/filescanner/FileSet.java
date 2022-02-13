package com.studio4plus.homerplayer.filescanner;

import android.net.Uri;

import com.google.common.base.Preconditions;

import java.io.File;

public class FileSet {

    public final String id;
    public final String name;
    public final Uri[] uris;
    public final boolean isDemoSample;

    public FileSet(String id, String name, Uri[] uris, boolean isDemoSample) {
        this.id = id;
        this.name = name;
        this.uris = uris;
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
