package com.studio4plus.homerplayer.ui;

import android.app.Activity;

import com.studio4plus.homerplayer.GlobalSettings;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;

@Module
public class ActivityModule {
    private final Activity activity;

    public ActivityModule(Activity activity) {
        this.activity = activity;
    }

    @Provides @ActivityScope
    Activity activity() {
        return activity;
    }

    @Provides @ActivityScope
    KioskModeHandler provideKioskModeHandler(
            Activity activity, GlobalSettings settings, EventBus eventBus) {
        return new KioskModeHandler(activity, settings, eventBus);
    }
}
