package com.studio4plus.homerplayer.downloads;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.MainThread;

import com.studio4plus.homerplayer.HomerPlayerApplication;


public class DownloadReceiver extends BroadcastReceiver {

    @Override
    @MainThread
    public void onReceive(final Context context, Intent intent) {
        switch(intent.getAction()) {
            case DownloadManager.ACTION_DOWNLOAD_COMPLETE: {
                final long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId != -1) {
                    SamplesDownloadController samplesDownloadController =
                            HomerPlayerApplication.getComponent(context).getSamplesDownloadController();
                    samplesDownloadController.processFinishedDownload(downloadId);
                }
            }
        }
    }

    public static void setEnabled(Context context, boolean enabled) {
        ComponentName receiver = new ComponentName(context, DownloadReceiver.class);
        PackageManager manager = context.getPackageManager();

        manager.setComponentEnabledSetting(
                receiver,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

}
