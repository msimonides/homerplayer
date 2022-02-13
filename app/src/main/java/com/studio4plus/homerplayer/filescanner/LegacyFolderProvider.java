package com.studio4plus.homerplayer.filescanner;

import android.content.Context;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.studio4plus.homerplayer.util.CollectionUtils;
import com.studio4plus.homerplayer.util.FilesystemUtil;

import java.io.File;
import java.util.List;

public class LegacyFolderProvider implements ScanFilesTask.FolderProvider {

    private static final String AUDIOBOOKS_FOLDER_NAME = "AudioBooks";

    private final Context appContext;

    public LegacyFolderProvider(@NonNull Context appContext) {
        this.appContext = appContext;
    }

    @Override
    @NonNull
    public List<File> getFolders() {
        List<File> rootDirs = FilesystemUtil.listRootDirs(appContext);
        File defaultStorage = Environment.getExternalStorageDirectory();
        if (!CollectionUtils.containsByValue(rootDirs, defaultStorage))
            rootDirs.add(defaultStorage);

        return CollectionUtils.map(rootDirs, (rootDir) -> new File(rootDir, AUDIOBOOKS_FOLDER_NAME));
    }
}
