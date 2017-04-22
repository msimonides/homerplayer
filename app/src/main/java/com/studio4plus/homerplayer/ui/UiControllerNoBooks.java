package com.studio4plus.homerplayer.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.downloads.DownloadService;
import com.studio4plus.homerplayer.downloads.SamplesDownloadController;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationFinishedEvent;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationStartedEvent;
import com.studio4plus.homerplayer.events.MediaStoreUpdateEvent;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class UiControllerNoBooks {

    public static class Factory {
        private final @NonNull Activity activity;
        private final @NonNull SamplesDownloadController samplesDownloadController;
        private final @NonNull EventBus eventBus;

        @Inject
        public Factory(@NonNull Activity activity,
                       @NonNull SamplesDownloadController samplesDownloadController,
                       @NonNull EventBus eventBus) {
            this.activity = activity;
            this.samplesDownloadController = samplesDownloadController;
            this.eventBus = eventBus;
        }

        public UiControllerNoBooks create(@NonNull NoBooksUi ui) {
            return new UiControllerNoBooks(activity, ui, samplesDownloadController, eventBus);
        }
    }

    private final @NonNull Activity activity;
    private final @NonNull NoBooksUi ui;
    private final @NonNull SamplesDownloadController samplesDownloadController;
    private final @NonNull EventBus eventBus;

    private @Nullable DownloadProgressReceiver progressReceiver;

    private UiControllerNoBooks(@NonNull Activity activity,
                                @NonNull NoBooksUi ui,
                                @NonNull SamplesDownloadController samplesDownloadController,
                                @NonNull EventBus eventBus) {
        this.activity = activity;
        this.ui = ui;
        this.samplesDownloadController = samplesDownloadController;
        this.eventBus = eventBus;

        ui.initWithController(this);

        if (samplesDownloadController.isDownloading())
            showInstallProgress();
    }

    public void startSamplesInstallation() {
        eventBus.post(new DemoSamplesInstallationStartedEvent());
        samplesDownloadController.startSamplesDownload();
        showInstallProgress();
    }

    public void abortSamplesInstallation() {
        Preconditions.checkState(samplesDownloadController.isDownloading());
        samplesDownloadController.cancelDownload();
        stopProgressReceiver();
    }

    void shutdown() {
        if (progressReceiver != null)
            stopProgressReceiver();
        ui.shutdown();
    }

    @SuppressWarnings("unused")
    public void onEvent(DemoSamplesInstallationFinishedEvent event) {
        Preconditions.checkNotNull(progressReceiver);
        Preconditions.checkNotNull(progressReceiver.observer);
        if (event.success) {
            eventBus.post(new MediaStoreUpdateEvent());
            stopProgressReceiver();
        } else {
            progressReceiver.observer.onFailure();
            stopProgressReceiver();
        }
    }

    private void showInstallProgress() {
        Preconditions.checkState(progressReceiver == null);

        NoBooksUi.InstallProgressObserver uiProgressObserver = ui.showInstallProgress();
        progressReceiver = new DownloadProgressReceiver(uiProgressObserver);
        IntentFilter filter = new IntentFilter(DownloadService.BROADCAST_DOWNLOAD_PROGRESS_ACTION);
        LocalBroadcastManager.getInstance(activity).registerReceiver(progressReceiver, filter);
        eventBus.register(this);
    }

    private void stopProgressReceiver() {
        Preconditions.checkState(progressReceiver != null);
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(progressReceiver);
        progressReceiver.stop();
        eventBus.unregister(this);
        progressReceiver = null;
    }

    private static class DownloadProgressReceiver extends BroadcastReceiver {

        private @Nullable NoBooksUi.InstallProgressObserver observer;

        DownloadProgressReceiver(@NonNull NoBooksUi.InstallProgressObserver observer) {
            this.observer = observer;
        }

        public void stop() {
            this.observer = null;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Preconditions.checkArgument(
                    DownloadService.BROADCAST_DOWNLOAD_PROGRESS_ACTION.equals(intent.getAction()));
            // Workaround for intents being sent after the receiver is unregistered:
            // https://code.google.com/p/android/issues/detail?id=191546
            if (observer == null)
                return;
            int transferredBytes = intent.getIntExtra(DownloadService.PROGRESS_BYTES_EXTRA, 0);
            int totalBytes = intent.getIntExtra(DownloadService.TOTAL_BYTES_EXTRA, -1);
            if (transferredBytes == totalBytes && totalBytes > -1) {
                observer.onInstallStarted();
            } else {
                observer.onDownloadProgress(transferredBytes, totalBytes);
            }
        }
    }
}
