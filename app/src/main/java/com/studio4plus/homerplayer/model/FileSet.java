package com.studio4plus.homerplayer.model;

import java.util.List;

public class FileSet {

    public final String id;
    public final String directoryName;
    public final List<String> filePaths;
    public final boolean isDemoSample;

    public FileSet(String id, String directoryName, List<String> filePaths, boolean isDemoSample) {
        this.id = id;
        this.directoryName = directoryName;
        this.filePaths = filePaths;
        this.isDemoSample = isDemoSample;
    }
}
