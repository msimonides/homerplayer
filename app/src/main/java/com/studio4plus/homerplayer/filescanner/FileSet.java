package com.studio4plus.homerplayer.filescanner;

import android.net.Uri;
import androidx.annotation.NonNull;

public class FileSet {

    public final String id;
    public final String name;
    public final Uri[] uris;
    public final boolean isDemoSample;

    public FileSet(@NonNull String id, @NonNull String name, @NonNull Uri[] uris, boolean isDemoSample) {
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
