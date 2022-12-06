package com.studio4plus.homerplayer.ui.settings;

import static com.studio4plus.homerplayer.util.CollectionUtils.filter;
import static com.studio4plus.homerplayer.util.CollectionUtils.map;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.studio4plus.homerplayer.ApplicationScope;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.concurrency.BackgroundExecutor;
import com.studio4plus.homerplayer.concurrency.SimpleFuture;
import com.studio4plus.homerplayer.events.MediaStoreUpdateEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

@ApplicationScope
public class AudiobooksFolderManager {

    private final Context context;
    private final ContentResolver contentResolver;
    private final GlobalSettings globalSettings;
    private final BackgroundExecutor ioExecutor;

    private final MutableLiveData<List<DocumentFile>> folders =
            new MutableLiveData<>(Collections.emptyList());

    @Inject
    public AudiobooksFolderManager(
            @Named("IO_EXECUTOR") @NonNull BackgroundExecutor ioExecutor,
            @NonNull EventBus eventBus,
            @NonNull Context context,
            @NonNull ContentResolver contentResolver,
            @NonNull GlobalSettings globalSettings) {
        this.ioExecutor = ioExecutor;
        this.context = context;
        this.contentResolver = contentResolver;
        this.globalSettings = globalSettings;
        eventBus.register(this);
        updateFolders();
    }

    @SuppressWarnings("UnusedDeclaration")
    @MainThread
    public void onEvent(MediaStoreUpdateEvent ignored) {
        updateFolders();
    }

    public void addAudiobooksFolder(@NonNull String newFolderUri) {
        Timber.i("Adding audiobooks folder: %s", newFolderUri);
        contentResolver.takePersistableUriPermission(Uri.parse(newFolderUri), Intent.FLAG_GRANT_READ_URI_PERMISSION);
        globalSettings.addAudiobooksFolder(newFolderUri);
        updateFolders();
    }

    public void removeAudiobooksFolder(@NonNull String removeFolderUri) {
        Timber.i("Removing audiobooks folder: %s", removeFolderUri);
        if (globalSettings.removeAudiobooksFolder(removeFolderUri)) {
            contentResolver.releasePersistableUriPermission(Uri.parse(removeFolderUri), Intent.FLAG_GRANT_READ_URI_PERMISSION);
            updateFolders();
        }
    }

    @NonNull
    public LiveData<List<DocumentFile>> observeFolders() {
        return folders;
    }

    @NonNull
    public List<DocumentFile> getCurrentFolders() {
        return folders.getValue();
    }

    public void updateFolders() {
        SimpleFuture<ValidationResult> future =
                ioExecutor.postTask(new ValidateFoldersTask(context, contentResolver, globalSettings.audiobooksFolders()));
        future.addListener(new SimpleFuture.Listener<ValidationResult>() {
            @Override
            public void onResult(@NonNull ValidationResult result) {
                folders.setValue(result.validFolders);

                for (String invalidUri : result.invalidFolderUris) {
                    try {
                        contentResolver.releasePersistableUriPermission(Uri.parse(invalidUri), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Throwable e) {
                        Timber.w(e, "Error while releasing permission to folder.");
                    }
                }
                globalSettings.removeAudiobooksFolders(result.invalidFolderUris);
            }

            @Override
            public void onException(@NonNull Throwable t) {
                Timber.e(t, "Unable to update audiobooks folders");
            }
        });
    }

    private static class ValidationResult {
        @NonNull
        public final List<DocumentFile> validFolders;
        @NonNull
        public final List<String> invalidFolderUris;

        private ValidationResult(@NonNull List<DocumentFile> validFolders, @NonNull List<String> invalidFolderUris) {
            this.validFolders = validFolders;
            this.invalidFolderUris = invalidFolderUris;
        }
    }

    private static class ValidateFoldersTask implements Callable<ValidationResult> {

        @NonNull
        private final Context context;
        @NonNull
        private final ContentResolver contentResolver;
        @NonNull
        private final Set<String> folderUris;


        private ValidateFoldersTask(@NonNull Context context, @NonNull ContentResolver contentResolver, @NonNull Set<String> folderUris) {
            this.context = context;
            this.contentResolver = contentResolver;
            this.folderUris = folderUris;
        }

        @Override
        public ValidationResult call() throws Exception {
            List<UriPermission> appPermissions =
                    filter(contentResolver.getPersistedUriPermissions(), UriPermission::isReadPermission);
            List<String> readPermissionUris = map(appPermissions, permission -> permission.getUri().toString());
            List<DocumentFile> validFolders = new ArrayList<>(folderUris.size());
            List<String> invalidUris = new ArrayList<>();
            for (String folderUri : folderUris) {
                if (readPermissionUris.contains(folderUri)) {
                    DocumentFile folderDocument = DocumentFile.fromTreeUri(context, Uri.parse(folderUri));
                    if (folderDocument != null && folderDocument.exists() && folderDocument.isDirectory()) {
                        validFolders.add(folderDocument);
                    } else {
                        Timber.w("Folder doesn't exist or URI doesn't point to a folder: %s", folderUri);
                        invalidUris.add(folderUri);
                    }
                } else {
                    Timber.w("No permission to access folder: %s", folderUri);
                    invalidUris.add(folderUri);
                }
            }

            return new ValidationResult(validFolders, invalidUris);
        }
    }
}
