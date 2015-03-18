package com.studio4plus.audiobookplayer.util;

import java.io.File;
import java.io.FileFilter;

public class DirectoryFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
        return pathname.isDirectory();
    }
}
