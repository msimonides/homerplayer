package com.studio4plus.homerplayer.ui.classic;

import android.support.v7.app.AppCompatActivity;

import com.studio4plus.homerplayer.ui.MainActivity;
import com.studio4plus.homerplayer.ui.MainUi;
import com.studio4plus.homerplayer.ui.ActivityScope;
import com.studio4plus.homerplayer.ui.SpeakerProvider;

import dagger.Module;
import dagger.Provides;

@Module
public class ClassicMainUiModule {
    private final MainActivity activity;

    public ClassicMainUiModule(MainActivity activity) {
        this.activity = activity;
    }

    @Provides @ActivityScope
    MainUi mainUi(AppCompatActivity activity) {
        return new ClassicMainUi(activity);
    }

    @Provides @ActivityScope
    SpeakerProvider speakProvider() {
        return activity;
    }
}
