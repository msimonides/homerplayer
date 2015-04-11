package com.studio4plus.homerplayer.service;

import android.content.Context;

import com.studio4plus.homerplayer.model.AudioBookManager;

import dagger.Module;
import dagger.Provides;

@Module
public class AudioBookPlayerModule {
    @Provides AudioBookPlayer provideAudioBookPlayer(Context context, AudioBookManager audioBookManager) {
        return new AudioBookPlayer(context, audioBookManager);
    }
}
