package com.studio4plus.homerplayer.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.SamplesMap;
import com.studio4plus.homerplayer.crashreporting.CrashReporting;
import com.studio4plus.homerplayer.service.DemoSamplesInstallerService;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationStartedEvent;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class UiControllerNoBooks {

    public static class Factory {
        private final @NonNull AppCompatActivity activity;
        private final @NonNull SamplesMap samples;
        private final @NonNull EventBus eventBus;

        @Inject
        public Factory(@NonNull AppCompatActivity activity,
                       @NonNull SamplesMap samples,
                       @NonNull EventBus eventBus) {
            this.activity = activity;
            this.samples = samples;
            this.eventBus = eventBus;
        }

        public UiControllerNoBooks create(@NonNull NoBooksUi ui) {
            return new UiControllerNoBooks(activity, ui, samples, eventBus);
        }
    }

    private static final String TAG = "UiControllerBooks";
    static final int PERMISSION_REQUEST_DOWNLOADS = 100;

    private final @NonNull AppCompatActivity activity;
    private final @NonNull NoBooksUi ui;
    private final @NonNull SamplesMap samples;
    private final @NonNull EventBus eventBus;

    private @Nullable DownloadProgressReceiver progressReceiver;

    private UiControllerNoBooks(@NonNull AppCompatActivity activity,
                                @NonNull NoBooksUi ui,
                                @NonNull SamplesMap samples,
                                @NonNull EventBus eventBus) {
        this.activity = activity;
        this.ui = ui;
        this.samples = samples;
        this.eventBus = eventBus;

        ui.initWithController(this);

        boolean isInstalling = DemoSamplesInstallerService.isInstalling();
        if (DemoSamplesInstallerService.isDownloading() || isInstalling)
            showInstallProgress(isInstalling);
    }

    public void startSamplesInstallation() {
        eventBus.post(new DemoSamplesInstallationStartedEvent());
        showInstallProgress(false);
        activity.startService(DemoSamplesInstallerService.createDownloadIntent(
                activity, samples.getSamples(activity.getResources().getConfiguration().locale.getLanguage())));
    }

    public void abortSamplesInstallation() {
        Preconditions.checkState(DemoSamplesInstallerService.isDownloading()
                || DemoSamplesInstallerService.isInstalling());
        CrashReporting.log(Log.INFO, TAG, "abortSamplesInstallation, isDownloading: " +
                DemoSamplesInstallerService.isDownloading());
        // Can't cancel installation.
        if (DemoSamplesInstallerService.isDownloading()) {
            activity.startService(DemoSamplesInstallerService.createCancelIntent(
                    activity));
            stopProgressReceiver();
        }
    }

    void shutdown() {
        if (progressReceiver != null)
            stopProgressReceiver();
        ui.shutdown();
    }

    private void showInstallProgress(boolean isAlreadyInstalling) {
        Preconditions.checkState(progressReceiver == null);
        CrashReporting.log(Log.INFO, TAG, "showInstallProgress, " +
                (isAlreadyInstalling ? "installation in progress" : "starting installation"));
        NoBooksUi.InstallProgressObserver uiProgressObserver =
                ui.showInstallProgress(isAlreadyInstalling);
        progressReceiver = new DownloadProgressReceiver(uiProgressObserver);
        IntentFilter filter = new IntentFilter();
        filter.addAction(DemoSamplesInstallerService.BROADCAST_DOWNLOAD_PROGRESS_ACTION);
        filter.addAction(DemoSamplesInstallerService.BROADCAST_INSTALL_STARTED_ACTION);
        filter.addAction(DemoSamplesInstallerService.BROADCAST_FAILED_ACTION);
        filter.addAction(DemoSamplesInstallerService.BROADCAST_INSTALL_FINISHED_ACTION);
        LocalBroadcastManager.getInstance(activity).registerReceiver(progressReceiver, filter);
    }

    private void stopProgressReceiver() {
        Preconditions.checkState(progressReceiver != null);
        CrashReporting.log(Log.INFO, TAG, "stopProgressReceiver");
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(progressReceiver);
        progressReceiver.stop();
        progressReceiver = null;
    }

    private class DownloadProgressReceiver extends BroadcastReceiver {

        private @Nullable NoBooksUi.InstallProgressObserver observer;

        DownloadProgressReceiver(@NonNull NoBooksUi.InstallProgressObserver observer) {
            this.observer = observer;
        }

        public void stop() {
            this.observer = null;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Preconditions.checkNotNull(intent.getAction());
            // Workaround for intents being sent after the receiver is unregistered:
            // https://code.google.com/p/android/issues/detail?id=191546
            if (observer == null)
                return;

            CrashReporting.log(Log.INFO, TAG, "progress receiver: " + intent.getAction());
            if (DemoSamplesInstallerService.BROADCAST_DOWNLOAD_PROGRESS_ACTION.equals(
                    intent.getAction())) {
                int transferredBytes = intent.getIntExtra(
                        DemoSamplesInstallerService.PROGRESS_BYTES_EXTRA, 0);
                int totalBytes = intent.getIntExtra(
                        DemoSamplesInstallerService.TOTAL_BYTES_EXTRA, -1);
                observer.onDownloadProgress(transferredBytes, totalBytes);
            } else if (DemoSamplesInstallerService.BROADCAST_INSTALL_STARTED_ACTION.equals(
                    intent.getAction())) {
                observer.onInstallStarted();
            } else if (DemoSamplesInstallerService.BROADCAST_INSTALL_FINISHED_ACTION.equals(
                    intent.getAction())) {
                stopProgressReceiver();
            } else if (DemoSamplesInstallerService.BROADCAST_FAILED_ACTION.equals(
                    intent.getAction())) {
                observer.onFailure();
                stopProgressReceiver();
            } else {
                Preconditions.checkState(false,
                        "Unexpected intent action: " + intent.getAction());
            }
        }
    }
}
