package com.studio4plus.homerplayer;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;

import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.model.FileScanner;
import com.studio4plus.homerplayer.model.Storage;

// A bag for globals...
public class HomerPlayerApplication extends Application {

    private static final String AUDIOBOOKS_DIRECTORY = "AudioBooks";
    private static AudioBookManager audioBookManager;

    private MediaScannerReceiver mediaScannerReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        Storage storage = new Storage(this);
        FileScanner fileScanner = new FileScanner(AUDIOBOOKS_DIRECTORY);
        audioBookManager = new AudioBookManager(fileScanner, storage);

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

    public static AudioBookManager getAudioBookManager() {
        return audioBookManager;
    }
}
