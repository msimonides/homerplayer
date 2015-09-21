package com.studio4plus.homerplayer.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downloader {

    public interface ProgressUpdater {
        void onDownloadProgress(int transferredBytes, int totalBytes);
    }

    private static final int BUFFER_SIZE = 65536;

    private final URL url;
    private ProgressUpdater progressUpdater;

    private volatile HttpURLConnection urlConnection;

    public Downloader(URL url) {
        this.url = url;
    }

    public void setProgressUpdater(ProgressUpdater progressUpdater) {
        this.progressUpdater = progressUpdater;
    }

    public boolean download(OutputStream outputStream) {
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            try {
                int totalBytes = urlConnection.getContentLength();
                InputStream in = urlConnection.getInputStream();

                int transferredBytes = 0;
                updateProgress(transferredBytes, totalBytes);

                byte buffer[] = new byte[BUFFER_SIZE];
                int readCount;
                while ((readCount = in.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, readCount);
                    transferredBytes += readCount;
                    updateProgress(transferredBytes, totalBytes);
                }
                updateProgress(transferredBytes, totalBytes);
                return true;
            } finally {
                urlConnection.disconnect();
            }
        } catch (IOException exception) {
            return false;
        }
    }

    // May be called on another thread.
    public void cancel() {
        if (urlConnection != null)
            urlConnection.disconnect();
    }

    private void updateProgress(int transferredBytes, int totalBytes) {
        if (progressUpdater != null)
            progressUpdater.onDownloadProgress(transferredBytes, totalBytes);
    }
}
