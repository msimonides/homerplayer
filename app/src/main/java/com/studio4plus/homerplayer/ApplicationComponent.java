package com.studio4plus.homerplayer;

import com.studio4plus.homerplayer.battery.BatteryStatusProvider;
import com.studio4plus.homerplayer.model.FileScanner;
import com.studio4plus.homerplayer.service.AudioBookPlayer;
import com.studio4plus.homerplayer.service.AudioBookPlayerModule;
import com.studio4plus.homerplayer.ui.BaseActivity;
import com.studio4plus.homerplayer.ui.BatteryStatusIndicator;
import com.studio4plus.homerplayer.ui.FragmentBookList;
import com.studio4plus.homerplayer.ui.FragmentNoBooks;
import com.studio4plus.homerplayer.ui.MainActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = { ApplicationModule.class, AudioBookManagerModule.class, AudioBookPlayerModule.class })
public interface ApplicationComponent {
    void inject(BaseActivity baseActivity);
    void inject(MainActivity mainActivity);
    void inject(FragmentBookList fragment);
    void inject(FragmentNoBooks fragment);
    void inject(BatteryStatusProvider batteryStatusProvider);
    void inject(BatteryStatusIndicator batteryStatusIndicator);
    AudioBookPlayer getAudioBookPlayer();
    FileScanner getFileScanner();
}
