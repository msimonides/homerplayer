package com.studio4plus.homerplayer.filescanner;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.studio4plus.homerplayer.util.CollectionUtils;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class ScanDocumentTreeTask implements Callable<List<FileSet>> {

    private static final String[] DOCUMENTS_PROJECTION = new String[]{
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
    };
    private static final String SORT_BY_NAME =
             DocumentsContract.Document.COLUMN_DISPLAY_NAME + " ASC";

    private static class FileItem {
        public final String path;
        public final Uri uri;
        public final long size;

        private FileItem(@NonNull String path, @NonNull Uri uri, long size) {
            this.path = path;
            this.uri = uri;
            this.size = size;
        }

        @NonNull
        public String getName() {
            int index = path.lastIndexOf("/");
            return index > -1 ? path.substring(index + 1) : path;
        }
    }

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
        long start = SystemClock.elapsedRealtime();
        for (Uri uri : audiobooksFolderUris) {
            filesets.addAll(scanAudiobooks(uri));
        }
        Collections.sort(filesets, (a, b) -> a.name.compareTo(b.name));
        long end = SystemClock.elapsedRealtime();
        Log.d("ListTest", "time: " + (end - start) + "ms");
        return filesets;
    }

    private List<FileSet> scanAudiobooks(@NonNull Uri treeUri) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri, DocumentsContract.getTreeDocumentId(treeUri));

        List<FileSet> fileSets = new ArrayList<>();
        try(Cursor cursor =
                    context.getContentResolver().query(childrenUri, DOCUMENTS_PROJECTION, null, null, null)) {
            while(cursor.moveToNext()) {
                if (isFolder(cursor.getString(2))) {
                    String name = cursor.getString(1);
                    List<FileItem> files = new ArrayList<>();
                    scanAudiobook(treeUri, cursor.getString(0), files, name);

                    if (!files.isEmpty()) {
                        Collections.sort(files, (a, b) -> a.path.compareTo(b.path));
                        Uri[] uris = CollectionUtils.map(files, f -> f.uri).toArray(new Uri[]{});
                        fileSets.add(new FileSet(generateId(files), name, uris, false));
                    }
                }
            }
        }
        return fileSets;
    }

    private void scanAudiobook(
            @NonNull Uri rootUri,
            @NonNull String folderDocId,
            @NonNull List<FileItem> files,
            @NonNull String path) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, folderDocId);

        try(Cursor cursor =
                    context.getContentResolver().query(childrenUri, DOCUMENTS_PROJECTION, null, null, SORT_BY_NAME)) {
            while(cursor.moveToNext()) {
                String filePath = path + "/" + cursor.getString(1);
                String documentId = cursor.getString(0);
                String mimeType = cursor.getString(2);
                if (isFolder(mimeType)) {
                    scanAudiobook(rootUri, documentId, files, filePath);
                } else if (mimeType.startsWith("audio/")) {
                    long size = cursor.getLong(3);
                    Uri uri = DocumentsContract.buildDocumentUriUsingTree(rootUri, documentId);
                    files.add(new FileItem(filePath, uri, size));
                }
            }
        }
    }

    private String generateId(@NonNull List<FileItem> files) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            ByteBuffer bufferLong = ByteBuffer.allocate(Long.SIZE);
            for (FileItem file : files) {
                bufferLong.putLong(0, file.size);
                digest.update(file.path.getBytes());
                digest.update(bufferLong);
            }
            return Base64.encodeToString(digest.digest(), Base64.NO_PADDING | Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Should never happen.
        }
    }

    private static boolean isFolder(@Nullable String mimeType) {
        return DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
    }
}
