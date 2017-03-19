package com.studio4plus.homerplayer.service;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class AudioBookPlayerModule {
    @Provides
    Player provideAudioBookPlayer() {
        return new Player();
    }
}
