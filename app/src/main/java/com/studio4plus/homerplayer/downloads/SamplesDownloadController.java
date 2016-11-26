package com.studio4plus.homerplayer.downloads;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationFinishedEvent;
import com.studio4plus.homerplayer.events.MediaStoreUpdateEvent;
import com.studio4plus.homerplayer.model.DemoSamplesInstaller;
import com.studio4plus.homerplayer.util.Callback;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import de.greenrobot.event.EventBus;

public class SamplesDownloadController {

    private final Context context;
    private final EventBus eventBus;
    private final Uri samplesDownloadUrl;

    private boolean isInstalling = false;

    @Inject
    public SamplesDownloadController(
            Context context,
            EventBus eventBus,
            @Named("SAMPLES_DOWNLOAD_URL") Uri samplesDownloadUrl) {
        this.context = context;
        this.eventBus = eventBus;
        this.samplesDownloadUrl = samplesDownloadUrl;

        IntentFilter filter = new IntentFilter(DownloadService.BROADCAST_DOWNLOAD_FINISHED_ACTION);
        // This causes the SamplesDownloadController to never be released but it's a singleton so
        // not a big deal.
        LocalBroadcastManager.getInstance(context).registerReceiver(
                new DownloadIntentListener(), filter);
    }

    @MainThread
    public void startSamplesDownload() {
        context.startService(DownloadService.createDownloadIntent(context, samplesDownloadUrl));
    }

    @MainThread
    public void onDownloadFinished(final File downloadedFile) {
        Log.d("SamplesDownloadControll", "Processing finished download.");
        if (downloadedFile != null) {
            DemoSamplesInstaller installer =
                    HomerPlayerApplication.getComponent(context).createDemoSamplesInstaller();
            Callback<Boolean> onFinished = new Callback<Boolean>() {
                @Override
                public void onFinished(Boolean success) {
                    SamplesDownloadController.this.onFinished(success);
                }
            };
            InstallTask installTask =
                    new InstallTask(installer, downloadedFile, onFinished);
            installTask.execute();
            isInstalling = true;
        } else {
            onFinished(false);
        }
    }

    @MainThread
    public void onFinished(boolean success) {
        isInstalling = false;
        eventBus.post(new DemoSamplesInstallationFinishedEvent(success));
        eventBus.post(new MediaStoreUpdateEvent());
    }

    @MainThread
    public boolean isDownloading() {
        return DownloadService.isDownloading() || isInstalling;
    }

    @MainThread
    public void cancelDownload() {
        if (isDownloading())
            context.startService(DownloadService.createCancelIntent(context));
    }

    private class DownloadIntentListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Preconditions.checkArgument(
                    DownloadService.BROADCAST_DOWNLOAD_FINISHED_ACTION.equals(intent.getAction()));
            File downloadedFile = null;
            String downloadedFilePath = intent.getStringExtra(DownloadService.DOWNLOAD_FILE_EXTRA);
            if (downloadedFilePath != null)
                downloadedFile = new File(downloadedFilePath);

            onDownloadFinished(downloadedFile);
        }
    }

    private static class InstallTask extends AsyncTask<Void, Void, Boolean> {

        private final @NonNull DemoSamplesInstaller installer;
        private final @NonNull File samplesZipPath;
        private final @NonNull Callback<Boolean> callback;

        private InstallTask(
                @NonNull DemoSamplesInstaller installer,
                @NonNull File samplesZipPath,
                @NonNull Callback<Boolean> finishedCallback) {
            this.installer = installer;
            this.samplesZipPath = samplesZipPath;
            this.callback = finishedCallback;
        }

        @Override
        @WorkerThread
        protected Boolean doInBackground(Void... params) {
            try {
                installer.installBooksFromZip(samplesZipPath);
                return true;
            } catch(Throwable t) {
                Crashlytics.logException(t);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            callback.onFinished(success);
        }
    }
}
