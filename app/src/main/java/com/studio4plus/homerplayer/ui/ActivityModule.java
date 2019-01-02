package com.studio4plus.homerplayer.ui;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.studio4plus.homerplayer.GlobalSettings;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;

@Module
public class ActivityModule {
    private final AppCompatActivity activity;

    public ActivityModule(@NonNull AppCompatActivity activity) {
        this.activity = activity;
    }

    @Provides @ActivityScope
    AppCompatActivity activity() {
        return activity;
    }

    @Provides @ActivityScope
    KioskModeHandler provideKioskModeHandler(
            AppCompatActivity activity, GlobalSettings settings, EventBus eventBus) {
        return new KioskModeHandler(activity, settings, eventBus);
    }
}
