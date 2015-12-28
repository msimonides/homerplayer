package com.studio4plus.homerplayer.downloads;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationFinished;
import com.studio4plus.homerplayer.events.MediaStoreUpdateEvent;
import com.studio4plus.homerplayer.model.DemoSamplesInstaller;
import com.studio4plus.homerplayer.util.Callback;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import de.greenrobot.event.EventBus;

/**
 * Encapsulates the logic for handling samples download.
 *
 * The class has no state and it may be created and destroyed on demand (so e.g.
 * startSamplesDownload() may be called on a different instance (or even different process)
 * than the subsequent processFinishedDownload()).
 * All state is persisted either in the DownloadManager or SharedPreferences.
 */
public class SamplesDownloadController {

    private final static String PREF_DOWNLOAD_ID = "SamplesDownloadController.downloadId";

    private final Context context;
    private final EventBus eventBus;
    private final DownloadManager downloadManager;
    private final Uri samplesDownloadUrl;
    private final SharedPreferences sharedPreferences;
    private final Resources resources;

    @Inject
    public SamplesDownloadController(
            Context context,
            EventBus eventBus,
            DownloadManager downloadManager,
            @Named("SAMPLES_DOWNLOAD_URL") Uri samplesDownloadUrl,
            SharedPreferences sharedPreferences, Resources resources) {
        this.context = context;
        this.eventBus = eventBus;
        this.downloadManager = downloadManager;
        this.samplesDownloadUrl = samplesDownloadUrl;
        this.sharedPreferences = sharedPreferences;
        this.resources = resources;
    }

    @MainThread
    public void startSamplesDownload() {
        DownloadReceiver.setEnabled(context, true);
        DownloadManager.Request request = new DownloadManager.Request(samplesDownloadUrl);
        request.setVisibleInDownloadsUi(false);
        request.setTitle(resources.getString(R.string.samplesDownloadNotificationTitle));
        long downloadId = downloadManager.enqueue(request);
        saveCurrentDownloadId(downloadId);
    }

    @MainThread
    public void processFinishedDownload(final long downloadId) {
        Log.d("SamplesDownloadControll", "Processing finished download.");
        final DownloadManager downloadManager =
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        final File downloadedFile =
                DownloadQueries.queryLocalFile(downloadManager, downloadId);
        if (downloadedFile != null) {
            DemoSamplesInstaller installer =
                    HomerPlayerApplication.getComponent(context).createDemoSamplesInstaller();
            Callback<Boolean> onFinished = new Callback<Boolean>() {
                @Override
                public void onFinished(Boolean success) {
                    SamplesDownloadController.this.onFinished(success, downloadId);
                }
            };
            InstallTask installTask =
                    new InstallTask(installer, downloadedFile, onFinished);
            installTask.execute();
        } else {
            onFinished(false, downloadId);
        }
    }

    @MainThread
    public void onFinished(boolean success, long downloadId) {
        eventBus.post(new DemoSamplesInstallationFinished(success));
        eventBus.post(new MediaStoreUpdateEvent());
        downloadManager.remove(downloadId);
        DownloadReceiver.setEnabled(context, false);
        clearCurrentDownloadId();
    }

    @MainThread
    public boolean isDownloading() {
        long downloadId = getCurrentDownloadId();
        if (downloadId == -1)
            return false;

        DownloadStatus downloadStatus =
                DownloadQueries.getDownloadStatus(downloadManager, downloadId);
        if (downloadStatus != null) {
            switch (downloadStatus.status) {
                case DownloadManager.STATUS_RUNNING:
                case DownloadManager.STATUS_PENDING:
                case DownloadManager.STATUS_SUCCESSFUL:
                case DownloadManager.STATUS_PAUSED:
                    return true;
                case DownloadManager.STATUS_FAILED:
                    clearCurrentDownloadId();
                    return false;
                default:
                    return false;
            }
        } else {
            clearCurrentDownloadId();
            return false;
        }
    }

    @MainThread
    public @Nullable DownloadStatus getDownloadProgress() {
        long downloadId = getCurrentDownloadId();
        Preconditions.checkState(downloadId != -1);

        return DownloadQueries.getDownloadStatus(downloadManager, downloadId);
    }

    @MainThread
    public void cancelDownload() {
        long downloadId = getCurrentDownloadId();
        Preconditions.checkState(downloadId != -1);

        downloadManager.remove(downloadId);
    }

    private long getCurrentDownloadId() {
        return sharedPreferences.getLong(PREF_DOWNLOAD_ID, -1);
    }

    private void saveCurrentDownloadId(long downloadId) {
        sharedPreferences.edit().putLong(PREF_DOWNLOAD_ID, downloadId).apply();
    }

    private void clearCurrentDownloadId() {
        sharedPreferences.edit().remove(PREF_DOWNLOAD_ID).apply();
    }

    private static class InstallTask extends AsyncTask<Void, Void, Boolean> {

        private final @NonNull DemoSamplesInstaller installer;
        private final @NonNull File samplesZipFile;
        private final @NonNull Callback<Boolean> callback;

        private InstallTask(
                @NonNull DemoSamplesInstaller installer,
                @NonNull File samplesZipFile,
                @NonNull Callback<Boolean> finishedCallback) {
            this.installer = installer;
            this.samplesZipFile = samplesZipFile;
            this.callback = finishedCallback;
        }

        @Override
        @WorkerThread
        protected Boolean doInBackground(Void... params) {
            try {
                installer.installBooksFromZip(samplesZipFile);
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
