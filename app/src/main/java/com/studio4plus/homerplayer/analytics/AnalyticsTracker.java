package com.studio4plus.homerplayer.analytics;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.events.AudioBooksChangedEvent;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationFinishedEvent;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationStartedEvent;
import com.studio4plus.homerplayer.events.SettingsEnteredEvent;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class AnalyticsTracker {

    private static final int ENTERED_SETTINGS_DIMENSION = 1;
    private static final int HAD_BOOKS_DIMENSION = 2;
    private static final String EVENT_SAMPLES_DOWNLOAD_STARTED = "SAMPLES_DOWNLOAD_STARTED";
    private static final String EVENT_SAMPLES_DOWNLOAD_FINISHED = "SAMPLES_DOWNLOAD_FINISHED";
    private static final String EVENT_SAMPLES_DOWNLOAD_SUCCESS = "SAMPLES_DOWNLOAD_SUCCESS";
    private static final String EVENT_SAMPLES_DOWNLOAD_FAILURE = "SAMPLES_DOWNLOAD_FAILURE";
    private static final String ONCE_OR_MORE = "At least once";
    private static final String NEVER = "Never";
    private static final String SAMPLE_BOOKS_ONLY = "Samples only";

    private final Tracker tracker;
    private final GoogleAnalytics googleAnalytics;
    private final FirebaseAnalytics firebaseAnalytics;
    private final GlobalSettings globalSettings;

    public enum EventCategory {
        BOOKS_INSTALLED,
        ONBOARDING
    }

    @Inject
    public AnalyticsTracker(
            Tracker tracker, GoogleAnalytics googleAnalytics, FirebaseAnalytics firebaseAnalytics,
            GlobalSettings globalSettings, EventBus eventBus) {
        this.tracker = tracker;
        this.googleAnalytics = googleAnalytics;
        this.firebaseAnalytics = firebaseAnalytics;
        this.globalSettings = globalSettings;
        eventBus.register(this);
    }

    public void sendScreenHit(String screenName) {
        tracker.setScreenName(screenName);
        tracker.send(new HitBuilders.ScreenViewBuilder()
                .setCustomDimension(ENTERED_SETTINGS_DIMENSION, settingsDimensionValue())
                .setCustomDimension(HAD_BOOKS_DIMENSION, installedBooksDimensionValue())
                .build());
        googleAnalytics.dispatchLocalHits();
    }

    public void sendEvent(EventCategory category, String actionName, boolean dispatchImmediately) {
        sendEvent(category, actionName, null, dispatchImmediately);
    }

    public void sendEvent(
            EventCategory category, String actionName, String label, boolean dispatchImmediately) {
        HitBuilders.EventBuilder builder =
                new HitBuilders.EventBuilder(category.name(), actionName);
        if (label != null)
            builder.setLabel(label);
        tracker.send(builder.build());
        if (dispatchImmediately)
            googleAnalytics.dispatchLocalHits();
    }

    @SuppressWarnings("unused")
    public void onEvent(AudioBooksChangedEvent event) {
        if (event.contentType.supersedes(globalSettings.booksEverInstalled())) {
            sendEvent(EventCategory.BOOKS_INSTALLED, event.contentType.name(), true);

            firebaseAnalytics.logEvent(
                    EventCategory.BOOKS_INSTALLED.name() + '_' + event.contentType.name(), null);
        }
        globalSettings.setBooksEverInstalled(event.contentType);
    }

    @SuppressWarnings("unused")
    public void onEvent(SettingsEnteredEvent event) {
        globalSettings.setSettingsEverEntered();
    }

    @SuppressWarnings("unused")
    public void onEvent(DemoSamplesInstallationStartedEvent event) {
        sendEvent(EventCategory.ONBOARDING, EVENT_SAMPLES_DOWNLOAD_STARTED, true);

        firebaseAnalytics.logEvent(EVENT_SAMPLES_DOWNLOAD_STARTED, null);
    }

    @SuppressWarnings("unused")
    public void onEvent(DemoSamplesInstallationFinishedEvent event) {
        sendEvent(
                EventCategory.ONBOARDING,
                EVENT_SAMPLES_DOWNLOAD_FINISHED,
                event.success ? "success" : "failure",
                true);

        firebaseAnalytics.logEvent(
                event.success ? EVENT_SAMPLES_DOWNLOAD_SUCCESS : EVENT_SAMPLES_DOWNLOAD_FAILURE,
                null);
    }

    private String settingsDimensionValue() {
        return globalSettings.settingsEverEntered() ? ONCE_OR_MORE : NEVER;
    }

    private String installedBooksDimensionValue() {
        switch(globalSettings.booksEverInstalled()) {
            case EMPTY:
                return NEVER;
            case SAMPLES_ONLY:
                return SAMPLE_BOOKS_ONLY;
            case USER_CONTENT:
                return ONCE_OR_MORE;
            default:
                return "unknown value";
        }
    }
}
