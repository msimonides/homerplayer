package com.studio4plus.homerplayer;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class HomerPlayerApplication extends Application {

    private static final String AUDIOBOOKS_DIRECTORY = "AudioBooks";

    private ApplicationComponent component;

    private MediaScannerReceiver mediaScannerReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        component = DaggerApplicationComponent.builder()
                .audioBookManagerModule(new AudioBookManagerModule(AUDIOBOOKS_DIRECTORY))
                .build();

        IntentFilter intentFilter =  new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addDataScheme("file");
        mediaScannerReceiver = new MediaScannerReceiver();
        registerReceiver(mediaScannerReceiver, intentFilter);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        unregisterReceiver(mediaScannerReceiver);
        mediaScannerReceiver = null;
    }

    public static ApplicationComponent getComponent(Context context) {
        return ((HomerPlayerApplication) context.getApplicationContext()).component;
    }
}
