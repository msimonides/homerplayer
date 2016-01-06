package com.studio4plus.homerplayer.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import java.io.File;

public class FilesystemUtil {

    public static File[] listRootDirs(Context context) {
        File[] filesDirs;
        if (Build.VERSION.SDK_INT < 19)
            filesDirs = new File[]{ context.getExternalFilesDir(null) };
        else
            filesDirs = API19.getExternalFilesDirs(context);

        File[] rootDirs = new File[filesDirs.length];
        for (int i = 0; i < filesDirs.length; ++i) {
            rootDirs[i] = getFSRootForPath(filesDirs[i]);
        }
        return rootDirs;
    }

    private static File getFSRootForPath(File path) {
        while (path != null && path.isDirectory()) {
            long fsSize = path.getTotalSpace();
            File parent = path.getParentFile();
            if (parent != null && parent.getTotalSpace() != fsSize)
                return path;
            if (parent != null)
                path = parent;
        }
        return path;
    }

    @TargetApi(19)
    private static class API19 {
        public static File[] getExternalFilesDirs(Context context) {
            return context.getExternalFilesDirs(null);
        }
    }
}
