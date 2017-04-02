package com.studio4plus.homerplayer.analytics;

import com.flurry.android.FlurryAgent;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.events.AudioBooksChangedEvent;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationFinishedEvent;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationStartedEvent;
import com.studio4plus.homerplayer.events.PlaybackProgressedEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppingEvent;
import com.studio4plus.homerplayer.events.SettingsEnteredEvent;
import com.studio4plus.homerplayer.model.AudioBook;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class AnalyticsTracker {
    private static final String BOOKS_INSTALLED = "booksInstalled";
    private static final String BOOKS_INSTALLED_TYPE_KEY = "type";
    private static final String BOOK_PLAYED = "bookPlayed";
    private static final String BOOK_PLAYED_TYPE_KEY = "type";
    private static final String BOOK_PLAYED_DURATION_KEY = "durationSec";
    private static final String BOOK_PLAYED_TYPE_SAMPLE = "sample";
    private static final String BOOK_PLAYED_TYPE_USER_CONTENT = "userContent";
    private static final String BOOK_SWIPED = "bookSwiped";
    private static final String FF_REWIND = "ffRewind";
    private static final String FF_REWIND_ABORTED = "ffRewindAborted";
    private static final String FF_REWIND_DURATION_KEY = "ffRewindDurationSec";
    private static final String FF_REWIND_IS_FF_KEY = "isFf";
    private static final String SAMPLES_DOWNLOAD_STARTED = "samplesDownloadStarted";
    private static final String SAMPLES_DOWNLOAD_SUCCESS = "samplesDownloadSuccess";
    private static final String SAMPLES_DOWNLOAD_FAILURE = "samplesDownloadFailure";

    private final GlobalSettings globalSettings;

    private CurrentlyPlayed currentlyPlayed;

    @Inject
    public AnalyticsTracker(
            GlobalSettings globalSettings, EventBus eventBus) {
        this.globalSettings = globalSettings;
        eventBus.register(this);
    }

    @SuppressWarnings("unused")
    public void onEvent(AudioBooksChangedEvent event) {
        if (event.contentType.supersedes(globalSettings.booksEverInstalled())) {
            Map<String, String> data = Collections.singletonMap(
                    BOOKS_INSTALLED_TYPE_KEY, event.contentType.name());
            FlurryAgent.logEvent(BOOKS_INSTALLED, data);
        }
        globalSettings.setBooksEverInstalled(event.contentType);
    }

    @SuppressWarnings("unused")
    public void onEvent(SettingsEnteredEvent event) {
        globalSettings.setSettingsEverEntered();
    }

    @SuppressWarnings("unused")
    public void onEvent(DemoSamplesInstallationStartedEvent event) {
        FlurryAgent.logEvent(SAMPLES_DOWNLOAD_STARTED);
    }

    @SuppressWarnings("unused")
    public void onEvent(DemoSamplesInstallationFinishedEvent event) {
        if (event.success) {
            FlurryAgent.logEvent(SAMPLES_DOWNLOAD_SUCCESS);
        } else {
            Map<String, String> data = Collections.singletonMap("error", event.errorMessage);
            FlurryAgent.logEvent(SAMPLES_DOWNLOAD_FAILURE, data);
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(PlaybackProgressedEvent event) {
        if (currentlyPlayed == null)
            currentlyPlayed = new CurrentlyPlayed(event.audioBook, System.nanoTime());
    }

    @SuppressWarnings("unused")
    public void onEvent(PlaybackStoppingEvent event) {
        if (currentlyPlayed != null) {
            Map<String, String> data = new HashMap<>();
            data.put(BOOK_PLAYED_TYPE_KEY,
                     currentlyPlayed.audioBook.isDemoSample()
                             ? BOOK_PLAYED_TYPE_SAMPLE
                             : BOOK_PLAYED_TYPE_USER_CONTENT);
            long elapsedTimeS = TimeUnit.NANOSECONDS.toSeconds(
                    System.nanoTime() - currentlyPlayed.startTimeNano);
            data.put(BOOK_PLAYED_DURATION_KEY, Long.toString(elapsedTimeS));
            currentlyPlayed = null;
            FlurryAgent.logEvent(BOOK_PLAYED, data);
        }
    }

    public void onBookSwiped() {
        FlurryAgent.logEvent(BOOK_SWIPED);
    }

    public void onFfRewindFinished(long runningTimeS, boolean isFf) {
        Map<String, String> data = new HashMap<>();
        data.put(FF_REWIND_DURATION_KEY, Long.toString(runningTimeS));
        data.put(FF_REWIND_IS_FF_KEY, Boolean.toString(isFf));
        FlurryAgent.logEvent(FF_REWIND, data);
    }

    public void onFfRewindAborted(boolean isFf) {
        Map<String, String> data =
                Collections.singletonMap(FF_REWIND_IS_FF_KEY, Boolean.toString(isFf));
        FlurryAgent.logEvent(FF_REWIND_ABORTED, data);
    }

    private static class CurrentlyPlayed {
        public final AudioBook audioBook;
        public final long startTimeNano;

        private CurrentlyPlayed(AudioBook audioBook, long startTimeNano) {
            this.audioBook = audioBook;
            this.startTimeNano = startTimeNano;
        }
    }
}
