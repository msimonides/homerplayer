package com.studio4plus.audiobookplayer;

import android.app.Application;

import com.studio4plus.audiobookplayer.model.AudioBookManager;
import com.studio4plus.audiobookplayer.model.Storage;

// A bag for globals...
public class AudioBookPlayerApplication extends Application {

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
