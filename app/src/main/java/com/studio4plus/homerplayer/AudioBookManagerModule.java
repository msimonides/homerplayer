package com.studio4plus.homerplayer;

import android.content.Context;

import androidx.annotation.NonNull;

import com.studio4plus.homerplayer.model.Storage;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;

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
    Storage provideStorage(@NonNull Context context, @NonNull EventBus eventBus) {
        return new Storage(context, eventBus);
    }
}
