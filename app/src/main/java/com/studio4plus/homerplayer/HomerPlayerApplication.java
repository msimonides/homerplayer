package com.studio4plus.homerplayer;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.provider.MediaStore;

import com.crittercism.app.Crittercism;

public class HomerPlayerApplication extends Application {

    private static final String AUDIOBOOKS_DIRECTORY = "AudioBooks";

    private ApplicationComponent component;
    private MediaStoreUpdateObserver mediaStoreUpdateObserver;

    @Override
    public void onCreate() {
        super.onCreate();

        Crittercism.initialize(getApplicationContext(), "553ffaa87365f84f7d3d7009");

        component = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .audioBookManagerModule(new AudioBookManagerModule(AUDIOBOOKS_DIRECTORY))
                .build();

        mediaStoreUpdateObserver = new MediaStoreUpdateObserver(new Handler(getMainLooper()));
        getContentResolver().registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreUpdateObserver);
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
