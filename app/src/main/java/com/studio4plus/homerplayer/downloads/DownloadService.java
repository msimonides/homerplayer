package com.studio4plus.homerplayer.downloads;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;

import com.google.common.base.Preconditions;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@MainThread
public class DownloadService extends Service {

    private static final String ACTION_EXTRA = "action";
    private static final int ACTION_START_DOWNLOAD = 0;
    private static final int ACTION_CANCEL_DOWNLOAD = 1;

    private static final int DOWNLOAD_BUFFER_SIZE = 32767;
    private static final long MIN_PROGRESS_UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(100);

    public static final String BROADCAST_DOWNLOAD_PROGRESS_ACTION =
            DownloadService.class.getName() + ".PROGRESS";
    public static final String PROGRESS_BYTES_EXTRA = "progressBytes";
    public static final String TOTAL_BYTES_EXTRA = "totalBytes";
    public static final String BROADCAST_DOWNLOAD_FINISHED_ACTION =
            DownloadService.class.getName() + ".FINISHED";
    public static final String DOWNLOAD_FILE_EXTRA = "file";
    public static final String DOWNLOAD_ERROR_EXTRA = "error";

    public static Intent createDownloadIntent(Context context, Uri downloadUri) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.setData(downloadUri);
        intent.putExtra(ACTION_EXTRA, ACTION_START_DOWNLOAD);
        return intent;
    }

    public static Intent createCancelIntent(Context context) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.putExtra(ACTION_EXTRA, ACTION_CANCEL_DOWNLOAD);
        return intent;
    }

    private DownloadThread currentDownloadThread;
    private long lastProgressUpdateNanos = 0;
    private static DownloadService instance;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int action = intent.getIntExtra(ACTION_EXTRA, -1);
        switch(action) {
            case ACTION_START_DOWNLOAD: {
                Preconditions.checkState(currentDownloadThread == null);
                String downloadUri = intent.getDataString();

                try {
                    ResultHandler result = new ResultHandler(this, getMainLooper());
                    currentDownloadThread = new DownloadThread(this, result, downloadUri);
                    currentDownloadThread.start();
                } catch (MalformedURLException e) {
                    onFailed(e.getMessage());
                }
                break;
            }
            case ACTION_CANCEL_DOWNLOAD:
                if (currentDownloadThread != null) {
                    currentDownloadThread.interrupt();
                    currentDownloadThread = null;
                }
                break;
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onDestroy() {
        if (currentDownloadThread != null)
            currentDownloadThread.interrupt();
        instance = null;
        super.onDestroy();
    }

    public static boolean isDownloading() {
        // This is a hack relying on the fact that only one service instance may be started
        // and that the service is local.
        return instance != null && instance.currentDownloadThread != null;
    }

    private void onFinished(@NonNull File destinationPath) {
        Intent intent = new Intent(BROADCAST_DOWNLOAD_FINISHED_ACTION);
        intent.putExtra(DOWNLOAD_FILE_EXTRA, destinationPath.getAbsolutePath());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        stopSelf();
    }

    private void onFailed(@NonNull String errorMessage) {
        Intent intent = new Intent(BROADCAST_DOWNLOAD_FINISHED_ACTION);
        intent.putExtra(DOWNLOAD_ERROR_EXTRA, errorMessage);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        stopSelf();
    }

    private void onProgress(int transferredBytes, int totalBytes) {
        long nowNanos = System.nanoTime();
        if (nowNanos - lastProgressUpdateNanos > MIN_PROGRESS_UPDATE_INTERVAL_NANOS
                || transferredBytes == totalBytes) {
            Intent intent = new Intent(BROADCAST_DOWNLOAD_PROGRESS_ACTION);
            intent.putExtra(PROGRESS_BYTES_EXTRA, transferredBytes);
            intent.putExtra(TOTAL_BYTES_EXTRA, totalBytes);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            lastProgressUpdateNanos = nowNanos;
        }
    }

    private static class ResultHandler {

        private final DownloadService service;
        private final Handler handler;

        private ResultHandler(DownloadService service, Looper looper) {
            this.service = service;
            this.handler = new Handler(looper);
        }

        void onFinished(final @NonNull File downloadPath) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    service.onFinished(downloadPath);
                }
            });
        }

        void onFailed(final @NonNull String errorMessage) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    service.onFailed(errorMessage);
                }
            });
        }

        void onProgress(final int progress, final int total) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    service.onProgress(progress, total);
                }
            });
        }
    }

    private static class DownloadThread extends Thread {

        private final Context context;
        private final ResultHandler resultHandler;
        private final URL downloadUrl;

        DownloadThread(Context context, ResultHandler resultHandler, String downloadUrl)
                throws MalformedURLException {
            super("DownloadThread");
            this.context = context;
            this.resultHandler = resultHandler;
            this.downloadUrl = new URL(downloadUrl);
        }

        @WorkerThread
        @Override
        public void run() {
            try {
                byte[] inputBuffer = new byte[DOWNLOAD_BUFFER_SIZE];
                File tmpFile = File.createTempFile("download", null, context.getExternalCacheDir());
                tmpFile.deleteOnExit();

                OutputStream output = new BufferedOutputStream(new FileOutputStream(tmpFile));
                HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
                InputStream input = new BufferedInputStream(connection.getInputStream());

                int totalBytesRead = 0;
                int bytesRead;
                while((bytesRead = input.read(inputBuffer, 0, inputBuffer.length)) > 0) {
                    output.write(inputBuffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    resultHandler.onProgress(totalBytesRead, connection.getContentLength());

                    if (isInterrupted())
                        break;
                }
                output.close();

                connection.disconnect();

                if (isInterrupted())
                    return;  // No result expected.

                resultHandler.onFinished(tmpFile);
            } catch (IOException e) {
                resultHandler.onFailed(e.getMessage());
            }
        }
    }
}
