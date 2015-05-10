package com.studio4plus.homerplayer;

import com.studio4plus.homerplayer.service.AudioBookPlayer;
import com.studio4plus.homerplayer.service.AudioBookPlayerModule;
import com.studio4plus.homerplayer.ui.FragmentBookList;
import com.studio4plus.homerplayer.ui.FragmentNoBooks;
import com.studio4plus.homerplayer.ui.MainActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = { ApplicationModule.class, AudioBookManagerModule.class, AudioBookPlayerModule.class })
public interface ApplicationComponent {
    void inject(MainActivity mainActivity);
    void inject(FragmentBookList fragment);
    void inject(FragmentNoBooks fragment);
    AudioBookPlayer getAudioBookPlayer();
}
