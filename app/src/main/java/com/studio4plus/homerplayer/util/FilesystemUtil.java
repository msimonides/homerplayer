package com.studio4plus.homerplayer.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FilesystemUtil {

    public static List<File> listRootDirs(Context context) {
        List<File> rootDirs = listStorageMounts();
        for (File rootDir : listSemiPermanentRootDirs(context)) {
            if (!rootDirs.contains(rootDir))
                rootDirs.add(rootDir);
        }

        return rootDirs;
    }

    private static File getFSRootForPath(File path) {
        while (path != null && path.isDirectory()) {
            long fsSize = path.getTotalSpace();
            File parent = path.getParentFile();
            if (parent == null || parent.getTotalSpace() != fsSize)
                return path;
            path = parent;
        }
        return path;
    }

    // Returns all system mount points that start with /storage.
    // This is likely to list attached SD cards, including those that are hidden from
    // Context.getExternalFilesDir()..
    // Some of the returned files may not be accessible due to permissions.
    private static List<File> listStorageMounts() {
        List<File> mounts = new ArrayList<>();
        File mountsFile = new File("/proc/mounts");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(mountsFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(" +");
                if (fields.length >= 2 && fields[1].startsWith("/storage")) {
                    mounts.add(new File(fields[1]));
                }
            }
        } catch (IOException e) {
            // Ignore, just return as much as is accumulated in mounts.
        }
        return mounts;
    }

    // Returns a list of file system roots on all semi-permanent storage mounts.
    // Semi-permanent storage is removable medium that is part of the device (e.g. an SD slot
    // inside the battery compartment) and therefore unlikely to be removed often.
    // Storage medium that is easily accessible by the user (e.g. an external SD card slot) is
    // treated as portable storage.
    // The Context.getExternalFilesDir() method only lists semi-permanent storage devices.
    //
    // See http://source.android.com/devices/storage/traditional.html#multiple-external-storage-devices
    private static List<File> listSemiPermanentRootDirs(Context context) {
        File[] filesDirs;
        if (Build.VERSION.SDK_INT < 19)
            filesDirs = new File[]{ context.getExternalFilesDir(null) };
        else
            filesDirs = API19.getExternalFilesDirs(context);

        List<File> rootDirs = new ArrayList<>(filesDirs.length);
        for (File path : filesDirs) {
            File root = getFSRootForPath(path);
            if (root != null)
                rootDirs.add(root);
        }
        return rootDirs;
    }

    @TargetApi(19)
    private static class API19 {
        public static File[] getExternalFilesDirs(Context context) {
            return context.getExternalFilesDirs(null);
        }
    }
}
