package com.studio4plus.homerplayer;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.provider.MediaStore;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.studio4plus.homerplayer.util.MediaScannerUtil;

import java.io.File;
import java.io.IOException;

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

        createAudioBooksDirectory(component.getAudioBookManager().getAudioBooksDirectory());
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
            if (path.mkdirs()) {
                // The MediaScanner doesn't work so well with directories (registers them as regular
                // files) so make it scan a dummy.
                final File dummyFile = new File(path, ".ignore");
                try {
                    if (dummyFile.createNewFile()) {
                        MediaScannerUtil.scanAndDeleteFile(this, dummyFile);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    // Just ignore.
                }
            }
        }
    }
}
