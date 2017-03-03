package com.studio4plus.homerplayer.analytics;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.events.AudioBooksChangedEvent;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationFinishedEvent;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationStartedEvent;
import com.studio4plus.homerplayer.events.SettingsEnteredEvent;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class AnalyticsTracker {
    private static final String EVENT_SAMPLES_DOWNLOAD_STARTED = "SAMPLES_DOWNLOAD_STARTED";
    private static final String EVENT_SAMPLES_DOWNLOAD_SUCCESS = "SAMPLES_DOWNLOAD_SUCCESS";
    private static final String EVENT_SAMPLES_DOWNLOAD_FAILURE = "SAMPLES_DOWNLOAD_FAILURE";

    private final FirebaseAnalytics firebaseAnalytics;
    private final GlobalSettings globalSettings;

    public enum EventCategory {
        BOOKS_INSTALLED
    }

    @Inject
    public AnalyticsTracker(
            FirebaseAnalytics firebaseAnalytics, GlobalSettings globalSettings, EventBus eventBus) {
        this.firebaseAnalytics = firebaseAnalytics;
        this.globalSettings = globalSettings;
        eventBus.register(this);
    }

    @SuppressWarnings("unused")
    public void onEvent(AudioBooksChangedEvent event) {
        if (event.contentType.supersedes(globalSettings.booksEverInstalled())) {
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
        firebaseAnalytics.logEvent(EVENT_SAMPLES_DOWNLOAD_STARTED, null);
    }

    @SuppressWarnings("unused")
    public void onEvent(DemoSamplesInstallationFinishedEvent event) {
        firebaseAnalytics.logEvent(
                event.success ? EVENT_SAMPLES_DOWNLOAD_SUCCESS : EVENT_SAMPLES_DOWNLOAD_FAILURE,
                null);
        // Firebase Analytics doesn't give any access to custom event parameters.
        // Log samples installation errors with Fabric for now.
        if (!event.success) {
            Answers.getInstance().logCustom(new CustomEvent(EVENT_SAMPLES_DOWNLOAD_FAILURE)
                    .putCustomAttribute("error", event.errorMessage));
        }
    }
}
