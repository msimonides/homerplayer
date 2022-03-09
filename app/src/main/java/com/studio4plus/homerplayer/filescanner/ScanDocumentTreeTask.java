package com.studio4plus.homerplayer.filescanner;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

public class ScanDocumentTreeTask implements Callable<List<FileSet>> {

    @NonNull
    private final Context context;
    @NonNull
    private final Collection<Uri> audiobooksFolderUris;

    public ScanDocumentTreeTask(
            @NonNull Context context,
            @NonNull Collection<Uri> audiobooksFolderUris) {
        this.context = context;
        this.audiobooksFolderUris = audiobooksFolderUris;
    }

    @Override
    public List<FileSet> call() throws Exception {
        List<FileSet> filesets = new ArrayList<>();
        for (Uri uri : audiobooksFolderUris) {
            DocumentFile folder = DocumentFile.fromTreeUri(context, uri);
            filesets.addAll(scanAudiobooks(folder.listFiles()));
        }
        return filesets;
    }

    private List<FileSet> scanAudiobooks(@NonNull DocumentFile[] audiobookFolders) {
        List<FileSet> fileSets = new ArrayList<>(audiobookFolders.length);
        for (DocumentFile audiobookFolder : audiobookFolders) {
            List<DocumentFile> audioFiles = new ArrayList<>();
            addFilesRecursive(audioFiles, audiobookFolder); // TODO: test DocumentContract.buildChildDocumentsUriUsingTree

            if (audioFiles.size() > 0) {
                ByteBuffer bufferLong = ByteBuffer.allocate(Long.SIZE);
                try {
                    List<Uri> uris = new ArrayList<>(audioFiles.size());
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    for (DocumentFile file : audioFiles) {
                        uris.add(file.getUri());

                        String parentName = file.getParentFile() != null ? file.getParentFile().getName() : null;
                        parentName = parentName != null ? parentName : "";
                        String name = file.getName() != null ? file.getName() : "";

                        // TODO: what if the same book is in two directories?
                        bufferLong.putLong(0, file.length());
                        digest.update(parentName.getBytes());
                        digest.update(name.getBytes());
                        digest.update(bufferLong);
                    }
                    String id = Base64.encodeToString(digest.digest(), Base64.NO_PADDING | Base64.NO_WRAP);
                    String name = Objects.requireNonNull(audiobookFolder.getName());
                    fileSets.add(new FileSet(id, name, uris.toArray(new Uri[]{}), false));
                } catch (NoSuchAlgorithmException e) {
                    // Never happens.
                    e.printStackTrace();
                    throw new RuntimeException("MD5 not available");
                }
            }
        }
        return fileSets;
    }

    private void addFilesRecursive(@NonNull List<DocumentFile> audioFiles, @NonNull DocumentFile folder) {
        DocumentFile[] files = folder.listFiles();
        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                addFilesRecursive(audioFiles, file);
            } else if (file.getType() != null && file.getType().startsWith("audio/")) {
                audioFiles.add(file);
            }
        }
    }
}
