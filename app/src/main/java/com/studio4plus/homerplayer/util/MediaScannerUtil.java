package com.studio4plus.homerplayer.util;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import java.io.File;

public class MediaScannerUtil {

    public static void scanAndDeleteFile(final Context context, final File file) {
        MediaScannerConnection.OnScanCompletedListener listener =
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        if (path == null)
                            return;

                        File scannedFile = new File(path);
                        if (scannedFile.equals(file)) {
                            //noinspection ResultOfMethodCallIgnored
                            file.delete();
                            MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, null);
                        }
                    }
                };

        MediaScannerConnection.scanFile(context, new String[] { file.getAbsolutePath() }, null, listener);
    }
}
