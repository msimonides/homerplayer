package com.studio4plus.homerplayer;

import android.app.Application;

import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.model.FileScanner;
import com.studio4plus.homerplayer.model.Storage;

// A bag for globals...
public class HomerPlayerApplication extends Application {

    private static final String AUDIOBOOKS_DIRECTORY = "AudioBooks";
    private static AudioBookManager audioBookManager;

    @Override
    public void onCreate() {
        super.onCreate();

        Storage storage = new Storage(this);
        FileScanner fileScanner = new FileScanner(AUDIOBOOKS_DIRECTORY);
        audioBookManager = new AudioBookManager(fileScanner, storage);
    }

    public static AudioBookManager getAudioBookManager() {
        return audioBookManager;
    }
}
