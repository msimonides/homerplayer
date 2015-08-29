package com.studio4plus.homerplayer;

import android.content.Context;

import com.studio4plus.homerplayer.model.Storage;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AudioBookManagerModule {

    private final String audioBooksDirectoryName;

    public AudioBookManagerModule(String audioBooksDirectoryName) {
        this.audioBooksDirectoryName = audioBooksDirectoryName;
    }

    @Provides @Named("AUDIOBOOKS_DIRECTORY")
    String provideAudioBooksDirectoryName() {
        return this.audioBooksDirectoryName;
    }

    @Provides @Singleton
    Storage provideStorage(Context context) {
        return new Storage(context);
    }
}
