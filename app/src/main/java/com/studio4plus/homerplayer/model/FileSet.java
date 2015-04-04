package com.studio4plus.homerplayer.model;

import java.util.List;

public class FileSet {

    public final String id;
    public final String directoryName;
    public final List<String> filePaths;

    public FileSet(String id, String directoryName, List<String> filePaths) {
        this.id = id;
        this.directoryName = directoryName;
        this.filePaths = filePaths;
    }
}
