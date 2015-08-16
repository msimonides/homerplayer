package com.studio4plus.homerplayer;

import android.app.Application;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Handler;
import android.provider.MediaStore;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import java.io.File;

import io.fabric.sdk.android.Fabric;

public class HomerPlayerApplication extends Application {

    private static final String AUDIOBOOKS_DIRECTORY = "AudioBooks";

    private ApplicationComponent component;
    private MediaStoreUpdateObserver mediaStoreUpdateObserver;

    @Override
    public void onCreate() {
        super.onCreate();

        CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
        Fabric.with(this, new Crashlytics.Builder().core(core).build());

        component = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .audioBookManagerModule(new AudioBookManagerModule(AUDIOBOOKS_DIRECTORY))
                .build();

        mediaStoreUpdateObserver = new MediaStoreUpdateObserver(new Handler(getMainLooper()));
        getContentResolver().registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreUpdateObserver);

        createAudioBooksDirectory(component.getFileScanner().getAudioBooksDirectory());
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

    private void createAudioBooksDirectory(File path) {
        if (!path.exists()) {
            if (path.mkdirs())
                MediaScannerConnection.scanFile(this, new String[] { path.getAbsolutePath() }, null, null);
        }
    }
}
