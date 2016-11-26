package com.studio4plus.homerplayer;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.studio4plus.homerplayer.analytics.AnalyticsTracker;
import com.studio4plus.homerplayer.downloads.SamplesDownloadController;

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

    @Provides
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
    GoogleAnalytics provideGoogleAnalytics(Context applicationContext) {
        return GoogleAnalytics.getInstance(applicationContext);
    }

    @Provides @Singleton
    Tracker provideGoogleAnalyticsTracker(GoogleAnalytics googleAnalytics) {
        return googleAnalytics.newTracker(R.xml.global_tracker);
    }

    @Provides @Singleton
    FirebaseAnalytics provideFirebaseTracker(Context context) {
        return FirebaseAnalytics.getInstance(context);
    }

    @Provides @Singleton
    AnalyticsTracker provideAnalyticsTracker(
            Tracker tracker, GoogleAnalytics googleAnalytics, FirebaseAnalytics firebaseAnalytics,
            GlobalSettings globalSettings, EventBus eventBus) {
        return new AnalyticsTracker(tracker, googleAnalytics, firebaseAnalytics, globalSettings, eventBus);
    }

    @Provides @Singleton
    SamplesDownloadController providerSamplesDownloadController(
            Context context, EventBus eventBus, @Named("SAMPLES_DOWNLOAD_URL") Uri samplesDownloadUrl) {
        return new SamplesDownloadController(context, eventBus, samplesDownloadUrl);
    }
}
