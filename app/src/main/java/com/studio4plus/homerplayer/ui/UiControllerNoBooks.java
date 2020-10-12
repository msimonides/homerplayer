package com.studio4plus.homerplayer.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.SamplesMap;
import com.studio4plus.homerplayer.analytics.AnalyticsTracker;
import com.studio4plus.homerplayer.service.DemoSamplesInstallerService;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationStartedEvent;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class UiControllerNoBooks {

    public static class Factory {
        private final @NonNull AppCompatActivity activity;
        private final @NonNull SamplesMap samples;
        private final @NonNull EventBus eventBus;
        private final @NonNull AnalyticsTracker analyticsTracker;

        @Inject
        public Factory(@NonNull AppCompatActivity activity,
                       @NonNull SamplesMap samples,
                       @NonNull EventBus eventBus,
                       @NonNull AnalyticsTracker analyticsTracker) {
            this.activity = activity;
            this.samples = samples;
            this.eventBus = eventBus;
            this.analyticsTracker = analyticsTracker;
        }

        public UiControllerNoBooks create(@NonNull NoBooksUi ui) {
            return new UiControllerNoBooks(activity, ui, samples, eventBus, analyticsTracker);
        }
    }

    private static final String TAG = "UiControllerBooks";
    static final int PERMISSION_REQUEST_DOWNLOADS = 100;

    private final @NonNull AppCompatActivity activity;
    private final @NonNull NoBooksUi ui;
    private final @NonNull SamplesMap samples;
    private final @NonNull EventBus eventBus;
    private final @NonNull AnalyticsTracker analyticsTracker;

    private @Nullable DownloadProgressReceiver progressReceiver;

    private UiControllerNoBooks(@NonNull AppCompatActivity activity,
                                @NonNull NoBooksUi ui,
                                @NonNull SamplesMap samples,
                                @NonNull EventBus eventBus,
                                @NonNull AnalyticsTracker analyticsTracker) {
        this.activity = activity;
        this.ui = ui;
        this.samples = samples;
        this.eventBus = eventBus;
        this.analyticsTracker = analyticsTracker;

        ui.initWithController(this);

        boolean isInstalling = DemoSamplesInstallerService.isInstalling();
        if (DemoSamplesInstallerService.isDownloading() || isInstalling)
            showInstallProgress(isInstalling);
    }

    public void startSamplesInstallation() {
        final boolean permissionsAlreadyGranted = PermissionUtils.checkAndRequestPermission(
                activity,
                new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE },
                PERMISSION_REQUEST_DOWNLOADS);
        Crashlytics.log(Log.DEBUG, TAG, "startSamplesInstallation, "
                + (permissionsAlreadyGranted ? "has permissions" : "requesting permissions"));
        if (permissionsAlreadyGranted)
            doStartSamplesInstallation();
    }

    private void doStartSamplesInstallation() {
        eventBus.post(new DemoSamplesInstallationStartedEvent());
        showInstallProgress(false);
        activity.startService(DemoSamplesInstallerService.createDownloadIntent(
                activity, samples.getSamples(activity.getResources().getConfiguration().locale.getLanguage())));
    }

    public void abortSamplesInstallation() {
        Preconditions.checkState(DemoSamplesInstallerService.isDownloading()
                || DemoSamplesInstallerService.isInstalling());
        Crashlytics.log(Log.DEBUG, TAG, "abortSamplesInstallation, isDownloading: " +
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

    void onRequestPermissionResult(int code, @NonNull int[] grantResults) {
        Preconditions.checkArgument(code == PERMISSION_REQUEST_DOWNLOADS);
        Preconditions.checkArgument(grantResults.length == 1);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            doStartSamplesInstallation();
        } else {
            boolean canRetry = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            AlertDialog.Builder dialogBuilder = PermissionUtils.permissionRationaleDialogBuilder(
                    activity, R.string.permission_rationale_download_samples);
            if (canRetry) {
                dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
            } else {
                analyticsTracker.onPermissionRationaleShown("downloadSamples");
                dialogBuilder.setPositiveButton(
                        R.string.permission_rationale_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        PermissionUtils.openAppSettings(activity);
                    }
                });
                dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
            }
            dialogBuilder.create().show();
        }
    }

    private void showInstallProgress(boolean isAlreadyInstalling) {
        Preconditions.checkState(progressReceiver == null);
        Crashlytics.log(Log.DEBUG, TAG, "showInstallProgress, " +
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
        Crashlytics.log(Log.DEBUG, TAG, "stopProgressReceiver");
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

            Crashlytics.log(Log.DEBUG, TAG, "progress receiver: " + intent.getAction());
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
