package com.studio4plus.homerplayer.ui.settings;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import javax.inject.Inject;

public class OnFolderSelected {

    private final AudiobooksFolderManager folderManager;

    @Inject
    public OnFolderSelected(@NonNull AudiobooksFolderManager folderManager) {
        this.folderManager = folderManager;
    }

    public void onSelected(@Nullable Uri uri) {
        if (uri != null) folderManager.addAudiobooksFolder(uri.toString());
    }
}
