package com.studio4plus.homerplayer;

import com.studio4plus.homerplayer.analytics.AnalyticsTracker;
import com.studio4plus.homerplayer.battery.BatteryStatusProvider;
import com.studio4plus.homerplayer.downloads.SamplesDownloadController;
import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.model.DemoSamplesInstaller;
import com.studio4plus.homerplayer.service.AudioBookPlayer;
import com.studio4plus.homerplayer.service.AudioBookPlayerModule;
import com.studio4plus.homerplayer.ui.BaseActivity;
import com.studio4plus.homerplayer.ui.BatteryStatusIndicator;
import com.studio4plus.homerplayer.ui.FragmentBookItem;
import com.studio4plus.homerplayer.ui.FragmentBookList;
import com.studio4plus.homerplayer.ui.FragmentNoBooks;
import com.studio4plus.homerplayer.ui.FragmentPlayback;
import com.studio4plus.homerplayer.ui.MainActivity;
import com.studio4plus.homerplayer.ui.SettingsActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = { ApplicationModule.class, AudioBookManagerModule.class, AudioBookPlayerModule.class })
public interface ApplicationComponent {
    void inject(BaseActivity baseActivity);
    void inject(MainActivity mainActivity);
    void inject(FragmentBookItem fragment);
    void inject(FragmentBookList fragment);
    void inject(FragmentNoBooks fragment);
    void inject(FragmentPlayback fragment);
    void inject(SettingsActivity.SettingsFragment fragment);
    void inject(BatteryStatusProvider batteryStatusProvider);
    void inject(BatteryStatusIndicator batteryStatusIndicator);
    AnalyticsTracker getAnalyticsTracker();
    AudioBookPlayer getAudioBookPlayer();
    AudioBookManager getAudioBookManager();
    DemoSamplesInstaller createDemoSamplesInstaller();
    SamplesDownloadController getSamplesDownloadController();
}
