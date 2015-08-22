package com.studio4plus.homerplayer.service;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.model.AudioBookManager;

import dagger.Module;
import dagger.Provides;

@Module
public class AudioBookPlayerModule {
    @Provides AudioBookPlayer provideAudioBookPlayer(
            GlobalSettings globalSettings, AudioBookManager audioBookManager) {
        return new AudioBookPlayer(globalSettings, audioBookManager);
    }
}
