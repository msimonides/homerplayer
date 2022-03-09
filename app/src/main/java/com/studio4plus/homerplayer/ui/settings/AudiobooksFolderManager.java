package com.studio4plus.homerplayer.ui.settings;

import static com.studio4plus.homerplayer.util.CollectionUtils.map;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;

import com.studio4plus.homerplayer.GlobalSettings;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class AudiobooksFolderManager {

    private final Context context;
    private final ContentResolver contentResolver;
    private final GlobalSettings globalSettings;
    private final SharedPreferences sharedPreferences;

    @Inject
    public AudiobooksFolderManager(
            @NonNull Context context,
            @NonNull ContentResolver contentResolver,
            @NonNull GlobalSettings globalSettings,
            @NonNull SharedPreferences sharedPreferences) {
        this.context = context;
        this.contentResolver = contentResolver;
        this.globalSettings = globalSettings;
        this.sharedPreferences = sharedPreferences;
    }

    public void addAudiobooksFolder(@NonNull String newFolderUri) {
        contentResolver.takePersistableUriPermission(Uri.parse(newFolderUri), Intent.FLAG_GRANT_READ_URI_PERMISSION);
        globalSettings.addAudiobooksFolder(newFolderUri);
    }

    public void removeAudiobooksFolder(@NonNull String removeFolderUri) {
        if (globalSettings.removeAudiobooksFolder(removeFolderUri)) {
            contentResolver.releasePersistableUriPermission(Uri.parse(removeFolderUri), Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    @NonNull
    public List<String> getFolderUris() {
        return new ArrayList<>(globalSettings.audiobooksFolders());
    }

    @NonNull
    public List<DocumentFile> getFolders() {
        return map(getFolderUris(), uri -> DocumentFile.fromTreeUri(context, Uri.parse(uri)));
    }

    @NonNull
    public LiveData<List<String>> observeFolders() {
        return new FoldersLiveData();
    }

    private class FoldersLiveData extends LiveData<List<String>>
        implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        protected void onActive() {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            setValue(getFolderUris());
        }

        @Override
        protected void onInactive() {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (GlobalSettings.KEY_AUDIOBOOKS_FOLDERS.equals(key))
                setValue(getFolderUris());
        }
    }
}
