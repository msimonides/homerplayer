package com.studio4plus.homerplayer;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.studio4plus.homerplayer.analytics.AnalyticsTracker;
import com.studio4plus.homerplayer.ui.HomeActivity;
import com.studio4plus.homerplayer.service.NotificationUtil;

import javax.inject.Inject;

import io.fabric.sdk.android.Fabric;

public class HomerPlayerApplication extends Application {

    private static final String AUDIOBOOKS_DIRECTORY = "AudioBooks";
    private static final String DEMO_SAMPLES_URL =
            "https://homer-player.firebaseapp.com/samples.zip";

    private ApplicationComponent component;
    private MediaStoreUpdateObserver mediaStoreUpdateObserver;

    @Inject public GlobalSettings globalSettings;
    @Inject public AnalyticsTracker analyticsTracker;  // Force creation of the tracker early.

    @Override
    public void onCreate() {
        super.onCreate();

        CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
        Fabric.with(this, new Crashlytics.Builder().core(core).build());

        component = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this, Uri.parse(DEMO_SAMPLES_URL)))
                .audioBookManagerModule(new AudioBookManagerModule(AUDIOBOOKS_DIRECTORY))
                .build();
        component.inject(this);

        mediaStoreUpdateObserver = new MediaStoreUpdateObserver(new Handler(getMainLooper()));
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
}
