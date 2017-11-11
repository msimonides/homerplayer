package com.studio4plus.homerplayer.service;

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

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationFinishedEvent;
import com.studio4plus.homerplayer.events.MediaStoreUpdateEvent;
import com.studio4plus.homerplayer.model.DemoSamplesInstaller;

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

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

@MainThread
public class DemoSamplesInstallerService extends Service {

    private static final String ACTION_EXTRA = "action";
    private static final int ACTION_START_DOWNLOAD = 0;
    private static final int ACTION_CANCEL_DOWNLOAD = 1;

    private static final int DOWNLOAD_BUFFER_SIZE = 32767;
    private static final long MIN_PROGRESS_UPDATE_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(100);

    public static final String BROADCAST_DOWNLOAD_PROGRESS_ACTION =
            DemoSamplesInstallerService.class.getName() + ".PROGRESS";
    public static final String PROGRESS_BYTES_EXTRA = "progressBytes";
    public static final String TOTAL_BYTES_EXTRA = "totalBytes";
    public static final String BROADCAST_INSTALL_STARTED_ACTION =
            DemoSamplesInstallerService.class.getName() + ".INSTALL_STARTED";
    public static final String BROADCAST_INSTALL_FINISHED_ACTION =
            DemoSamplesInstallerService.class.getName() + ".INSTALL_FINISHED";
    public static final String BROADCAST_FAILED_ACTION =
            DemoSamplesInstallerService.class.getName() + ".FAILED";

    public static Intent createDownloadIntent(Context context, Uri downloadUri) {
        Intent intent = new Intent(context, DemoSamplesInstallerService.class);
        intent.setData(downloadUri);
        intent.putExtra(ACTION_EXTRA, ACTION_START_DOWNLOAD);
        return intent;
    }

    public static Intent createCancelIntent(Context context) {
        Intent intent = new Intent(context, DemoSamplesInstallerService.class);
        intent.putExtra(ACTION_EXTRA, ACTION_CANCEL_DOWNLOAD);
        return intent;
    }

    // It's a bit ugly the the service communicates with other components both via
    // a LocalBroadcastManager and an EventBus.
    @Inject public EventBus eventBus;
    private DownloadAndInstallThread downloadAndInstallThread;
    private boolean isDownloading = false;
    private long lastProgressUpdateNanos = 0;
    private static DemoSamplesInstallerService instance;

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
                Crashlytics.log("DemoSamplesInstallerService: starting download");
                Preconditions.checkState(downloadAndInstallThread == null);
                String downloadUri = intent.getDataString();

                try {
                    ResultHandler result = new ResultHandler(this, getMainLooper());
                    isDownloading = true;
                    downloadAndInstallThread = new DownloadAndInstallThread(this, result, downloadUri);
                    downloadAndInstallThread.start();
                } catch (MalformedURLException e) {
                    onFailed(e.getMessage());
                }
                break;
            }
            case ACTION_CANCEL_DOWNLOAD:
                Crashlytics.log("DemoSamplesInstallerService: cancelling download");
                if (downloadAndInstallThread != null) {
                    isDownloading = false;
                    downloadAndInstallThread.interrupt();
                    downloadAndInstallThread = null;
                }
                break;
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Crashlytics.log("DemoSamplesInstallerService: created");
        HomerPlayerApplication.getComponent(getApplicationContext()).inject(this);
        instance = this;
    }

    @Override
    public void onDestroy() {
        Crashlytics.log("DemoSamplesInstallerService: destroying");
        if (downloadAndInstallThread != null)
            downloadAndInstallThread.interrupt();
        instance = null;
        super.onDestroy();
    }

    public static boolean isDownloading() {
        // This is a hack relying on the fact that only one service instance may be started
        // and that the service is local.
        return instance != null && instance.isDownloading;
    }

    public static boolean isInstalling() {
        // This is a hack relying on the fact that only one service instance may be started
        // and that the service is local.
        return instance != null && instance.downloadAndInstallThread != null
                && !instance.isDownloading;
    }

    private void onInstallStarted() {
        Crashlytics.log("DemoSamplesInstallerService: install started");
        isDownloading = false;
        Intent intent = new Intent(BROADCAST_INSTALL_STARTED_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void onInstallFinished() {
        Crashlytics.log("DemoSamplesInstallerService: install finished");
        Intent intent = new Intent(BROADCAST_INSTALL_FINISHED_ACTION);
        eventBus.post(new DemoSamplesInstallationFinishedEvent(true, null));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        // MediaStoreUpdateEvent may change the state of the UI, send it as the last action.
        eventBus.post(new MediaStoreUpdateEvent());
        stopSelf();
    }

    private void onFailed(@NonNull String errorMessage) {
        Crashlytics.log("DemoSamplesInstallerService: download or install failed");
        isDownloading = false;
        eventBus.post(new DemoSamplesInstallationFinishedEvent(true, errorMessage));
        Intent intent = new Intent(BROADCAST_FAILED_ACTION);
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

        private final DemoSamplesInstallerService service;
        private final Handler handler;

        private ResultHandler(DemoSamplesInstallerService service, Looper looper) {
            this.service = service;
            this.handler = new Handler(looper);
        }

        void onInstallFinished() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    service.onInstallFinished();
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

        void onDownloadProgress(final int progress, final int total) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    service.onProgress(progress, total);
                }
            });
        }

        void onInstallStarted() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    service.onInstallStarted();
                }
            });
        }
    }

    private static class DownloadAndInstallThread extends Thread {

        private final Context context;
        private final ResultHandler resultHandler;
        private final URL downloadUrl;

        DownloadAndInstallThread(Context context, ResultHandler resultHandler, String downloadUrl)
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
                File tmpFile = downloadSamples();

                if (isInterrupted())
                    return;  // No result expected.

                resultHandler.onInstallStarted();
                DemoSamplesInstaller installer =
                        HomerPlayerApplication.getComponent(context).createDemoSamplesInstaller();
                installer.installBooksFromZip(tmpFile);

                resultHandler.onInstallFinished();
            } catch (IOException e) {
                resultHandler.onFailed(e.getMessage());
            }
        }

        @WorkerThread
        private File downloadSamples() throws IOException {
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
                resultHandler.onDownloadProgress(totalBytesRead, connection.getContentLength());

                if (isInterrupted())
                    break;
            }
            output.close();

            connection.disconnect();

            return tmpFile;
        }
    }
}
