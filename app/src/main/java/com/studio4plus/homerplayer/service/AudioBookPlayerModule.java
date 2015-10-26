package com.studio4plus.homerplayer.service;

import com.studio4plus.homerplayer.GlobalSettings;

import dagger.Module;
import dagger.Provides;

@Module
public class AudioBookPlayerModule {
    @Provides AudioBookExoPlayer provideAudioBookPlayer(GlobalSettings globalSettings) {
        return new AudioBookExoPlayer(globalSettings);
    }
}
