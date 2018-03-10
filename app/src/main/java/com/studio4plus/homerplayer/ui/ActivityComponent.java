package com.studio4plus.homerplayer.ui;

import android.app.Activity;

import com.studio4plus.homerplayer.ApplicationComponent;

import dagger.Component;

@ActivityScope
@Component(dependencies = ApplicationComponent.class, modules = ActivityModule.class)
public interface ActivityComponent {
    Activity activity();
    void inject(SettingsActivity settingsActivity);
}
