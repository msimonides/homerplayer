package com.studio4plus.homerplayer.model;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.List;

public class FileSet {

    public final String id;
    public final File absolutePath;
    public final String directoryName;
    public final List<String> filePaths;
    public final boolean isDemoSample;

    public FileSet(String id, File absolutePath, List<String> filePaths, boolean isDemoSample) {
        Preconditions.checkArgument(absolutePath.isDirectory());
        this.id = id;
        this.absolutePath = absolutePath;
        this.directoryName = absolutePath.getName();
        this.filePaths = filePaths;
        this.isDemoSample = isDemoSample;
    }
}
