package com.studio4plus.homerplayer.util;

import java.io.File;
import java.io.FileFilter;

public class OrFilter implements FileFilter {

    private final FileFilter[] filters;

    public OrFilter(FileFilter... filters) {
        this.filters = filters;
    }

    @Override
    public boolean accept(File pathname) {
        for (FileFilter filter : filters) {
            if (filter.accept(pathname))
                return true;
        }
        return false;
    }
}
