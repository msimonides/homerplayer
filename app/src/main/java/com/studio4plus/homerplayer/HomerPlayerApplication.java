package com.studio4plus.homerplayer;

import android.app.Application;

import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.model.Storage;

// A bag for globals...
public class HomerPlayerApplication extends Application {

    private static AudioBookManager audioBookManager;

    @Override
    public void onCreate() {
        super.onCreate();

        Storage storage = new Storage(this);
        audioBookManager = new AudioBookManager(storage);
    }

    public static AudioBookManager getAudioBookManager() {
        return audioBookManager;
    }
}
