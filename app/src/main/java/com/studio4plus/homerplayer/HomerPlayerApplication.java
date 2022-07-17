package com.studio4plus.homerplayer;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.MediaStore;

import androidx.core.content.pm.PackageInfoCompat;
import androidx.multidex.MultiDexApplication;

import com.michaelflisar.lumberjack.FileLoggingSetup;
import com.michaelflisar.lumberjack.FileLoggingTree;
import com.studio4plus.homerplayer.analytics.AnalyticsTracker;
import com.studio4plus.homerplayer.crashreporting.CrashReporting;
import com.studio4plus.homerplayer.ui.HomeActivity;
import com.studio4plus.homerplayer.service.NotificationUtil;

import javax.inject.Inject;

import timber.log.Timber;

public class HomerPlayerApplication extends MultiDexApplication {

    private static final String AUDIOBOOKS_DIRECTORY = "AudioBooks";

    private ApplicationComponent component;

    @Inject public MediaStoreUpdateObserver mediaStoreUpdateObserver;
    @Inject public GlobalSettings globalSettings;
    @Inject public AnalyticsTracker analyticsTracker;  // Force creation of the tracker early.
    @Inject public FileLoggingSetup fileLoggingSetup;

    @Override
    public void onCreate() {
        super.onCreate();

        if (!BuildConfig.DEBUG) {
            CrashReporting.init(this);
        }

        component = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .audioBookManagerModule(new AudioBookManagerModule(AUDIOBOOKS_DIRECTORY))
                .build();
        component.inject(this);

        Timber.plant(new FileLoggingTree(fileLoggingSetup));
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        Timber.i("Application started");

        long currentVersionCode = getVersionCode();
        long previousVersionCode = globalSettings.lastVersionCode();
        if (isUpdate(previousVersionCode, currentVersionCode)) {
            onUpdate(previousVersionCode);
        }
        if (previousVersionCode < currentVersionCode) {
            // This is true both for fresh installations and updates.
            globalSettings.setLastVersionCode(currentVersionCode);
        }

        getContentResolver().registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreUpdateObserver);

        HomeActivity.setEnabled(this, globalSettings.isAnyKioskModeEnabled());

        if (Build.VERSION.SDK_INT >= 26)
            NotificationUtil.API26.registerPlaybackServiceChannel(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        getContentResolver().unregisterContentObserver(mediaStoreUpdateObserver);
        mediaStoreUpdateObserver = null;
    }

    public static ApplicationComponent getComponent(Context context) {
        return ((HomerPlayerApplication) context.getApplicationContext()).component;
    }

    private boolean isUpdate(long previousVersionCode, long currentVersionCode) {
        if (previousVersionCode == 0) {
            // previousVersionCode is 0 for versions up to 55 so it can't be used to distinguish
            // between an update and a new installation. browsingHintShown is a good approximation.
            return globalSettings.browsingHintShown();
        } else {
            return previousVersionCode < currentVersionCode;
        }
    }

    private void onUpdate(long previousVersionCode) {
        if (previousVersionCode < 56) {
            globalSettings.setVolumeControlsEnabled(false);
        } else if (previousVersionCode < 63) {
            globalSettings.setLegacyFileAccessMode(true);
        }
    }

    private long getVersionCode() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return PackageInfoCompat.getLongVersionCode(info);
        } catch (PackageManager.NameNotFoundException e) {
            // Should never happen.
            CrashReporting.logException(e);
            return 0L;
        }
    }
}
