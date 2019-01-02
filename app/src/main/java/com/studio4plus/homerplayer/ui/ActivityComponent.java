package com.studio4plus.homerplayer.ui;

import android.support.v7.app.AppCompatActivity;

import com.studio4plus.homerplayer.ApplicationComponent;
import com.studio4plus.homerplayer.ui.settings.SettingsActivity;

import dagger.Component;

@ActivityScope
@Component(dependencies = ApplicationComponent.class, modules = ActivityModule.class)
public interface ActivityComponent {
    AppCompatActivity activity();
    void inject(SettingsActivity settingsActivity);
}
