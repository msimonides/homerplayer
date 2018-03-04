package com.studio4plus.homerplayer;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;

import com.studio4plus.homerplayer.analytics.AnalyticsTracker;
import com.studio4plus.homerplayer.concurrency.BackgroundExecutor;

import java.util.Locale;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;

@Module
public class ApplicationModule {

    private final Application application;
    private final Uri samplesDownloadUrl;

    public ApplicationModule(Application application, Uri samplesDownloadUrl) {
        this.application = application;
        this.samplesDownloadUrl = samplesDownloadUrl;
    }

    @Provides @ApplicationScope
    Context provideContext() {
        return application;
    }

    @Provides
    Resources provideResources(Context context) {
        return context.getResources();
    }

    @Provides
    Locale provideCurrentLocale(Resources resources) {
        return resources.getConfiguration().locale;
    }

    @Provides
    SharedPreferences provideSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides @Singleton @Named("SAMPLES_DOWNLOAD_URL")
    Uri provideSamplesUrl() {
        return samplesDownloadUrl;
    }

    @Provides
    EventBus provideEventBus() {
        // TODO: provide the EventBus to all classes via Dagger and then switch to a private instance.
        return EventBus.getDefault();
    }

    @Provides @Singleton
    AnalyticsTracker provideAnalyticsTracker(GlobalSettings globalSettings, EventBus eventBus) {
        return new AnalyticsTracker(globalSettings, eventBus);
    }

    @Provides @Singleton @Named("IO_EXECUTOR")
    BackgroundExecutor provideIoExecutor(Context applicationContext) {
        HandlerThread ioThread = new HandlerThread("IO");
        ioThread.start();
        Handler ioHandler = new Handler(ioThread.getLooper());
        return new BackgroundExecutor(new Handler(applicationContext.getMainLooper()), ioHandler);
    }
}
