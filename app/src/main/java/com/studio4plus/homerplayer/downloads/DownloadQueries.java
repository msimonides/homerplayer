package com.studio4plus.homerplayer.downloads;

import android.app.DownloadManager;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import java.io.File;

public class DownloadQueries {

    public static @Nullable Uri queryContentUri(
            DownloadManager downloadManager, long downloadId) {
        Cursor cursor = getDownloadRowById(downloadManager, downloadId);

        if (cursor != null) {
            int filenameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
            String uri = cursor.getString(filenameIndex);
            cursor.close();
            return uri != null ? Uri.parse(uri) : null;
        } else {
            return null;
        }
    }

    public static @Nullable DownloadStatus getDownloadStatus(
            DownloadManager downloadManager, long downloadId) {
        Cursor cursor = getDownloadRowById(downloadManager, downloadId);
        if (cursor != null) {
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            int transferredBytesIndex =
                    cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
            DownloadStatus downloadStatus = new DownloadStatus(
                    cursor.getInt(statusIndex),
                    cursor.getInt(transferredBytesIndex),
                    cursor.getInt(totalBytesIndex));
            cursor.close();
            return downloadStatus;
        } else {
            return null;
        }
    }

    private static @Nullable Cursor getDownloadRowById(
            DownloadManager downloadManager, long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);

        return cursor.moveToFirst() ? cursor : null;
    }
}
