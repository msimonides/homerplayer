package com.studio4plus.homerplayer.ui;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.analytics.AnalyticsTracker;
import com.studio4plus.homerplayer.events.PlaybackProgressedEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppingEvent;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.service.PlaybackService;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class UiControllerPlayback {

    private static final String TAG = "UiControllerPlayback";

    static class Factory {
        private final @NonNull EventBus eventBus;
        private final @NonNull AnalyticsTracker analyticsTracker;

        @Inject
        Factory(@NonNull EventBus eventBus, @NonNull AnalyticsTracker analyticsTracker) {
            this.eventBus = eventBus;
            this.analyticsTracker = analyticsTracker;
        }

        UiControllerPlayback create(
                @NonNull PlaybackService playbackService, @NonNull PlaybackUi ui) {
            return new UiControllerPlayback(eventBus, analyticsTracker, playbackService, ui);
        }
    }

    private final @NonNull EventBus eventBus;
    private final @NonNull AnalyticsTracker analyticsTracker;
    private final @NonNull Handler mainHandler;
    private final @NonNull PlaybackService playbackService;
    private final @NonNull PlaybackUi ui;

    // Non-null only when rewinding.
    private @Nullable FFRewindController ffRewindController;

    private UiControllerPlayback(@NonNull EventBus eventBus,
                         @NonNull AnalyticsTracker analyticsTracker,
                         @NonNull PlaybackService playbackService,
                         @NonNull PlaybackUi playbackUi) {
        this.eventBus = eventBus;
        this.analyticsTracker = analyticsTracker;
        this.playbackService = playbackService;
        this.ui = playbackUi;
        this.mainHandler = new Handler(Looper.getMainLooper());

        ui.initWithController(this);

        eventBus.register(this);

        if (playbackService.getState() == PlaybackService.State.PLAYBACK) {
            ui.onPlaybackProgressed(playbackService.getCurrentTotalPositionMs());
        }
    }

    void shutdown() {
        stopRewindIfActive();
        eventBus.unregister(this);
    }

    void stopRewindIfActive() {
        if (ffRewindController != null)
            stopRewind();
    }

    void startPlayback(@NonNull AudioBook book) {
        playbackService.startPlayback(book);
    }

    public void stopPlayback() {
        Crashlytics.log(Log.DEBUG, TAG, "UiControllerPlayback.stopPlayback");
        playbackService.stopPlayback();
    }

    @SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
    public void onEvent(PlaybackStoppingEvent event) {
        ui.onPlaybackStopping();
    }

    @SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
    public void onEvent(PlaybackProgressedEvent event) {
        ui.onPlaybackProgressed(event.playbackPositionMs);
    }

    public void pauseForRewind() {
        playbackService.pauseForRewind();
    }

    public void resumeFromRewind() {
        playbackService.resumeFromRewind();
    }

    public void startRewind(boolean isForward, @NonNull FFRewindTimer.Observer timerObserver) {
        Preconditions.checkState(ffRewindController == null);

        ffRewindController = new FFRewindController(
                mainHandler,
                ui,
                playbackService.getCurrentTotalPositionMs(),
                playbackService.getAudioBookBeingPlayed().getTotalDurationMs(),
                isForward,
                timerObserver);
        ffRewindController.start();
        analyticsTracker.onFfRewindStarted(isForward);
    }

    public void stopRewind() {
        if (ffRewindController != null) {
            analyticsTracker.onFfRewindFinished(ffRewindController.getRewindWallTimeMs());
            playbackService.getAudioBookBeingPlayed().updateTotalPosition(
                    ffRewindController.getDisplayTimeMs());
            ffRewindController.stop();
            ffRewindController = null;
        } else {
            analyticsTracker.onFfRewindAborted();
        }
    }

    private static class FFRewindController implements FFRewindTimer.Observer {
        private static final int[] SPEED_LEVEL_SPEEDS = { 250, 100, 25  };
        private static final long[] SPEED_LEVEL_THRESHOLDS = { 15_000, 90_000, Long.MAX_VALUE };
        private static final PlaybackUi.SpeedLevel[] SPEED_LEVELS =
                {PlaybackUi.SpeedLevel.REGULAR, PlaybackUi.SpeedLevel.FAST, PlaybackUi.SpeedLevel.FASTEST };

        private final @NonNull PlaybackUi ui;
        private final @NonNull FFRewindTimer timer;

        private long startTimeNano;
        private long initialDisplayTimeMs;
        private int currentSpeedLevelIndex = -1;

        final boolean isFF;

        FFRewindController(
                @NonNull Handler handler,
                @NonNull PlaybackUi ui,
                long currentTotalPositionMs,
                long maxTotalPositionMs,
                boolean isFF,
                @NonNull FFRewindTimer.Observer timerObserver) {
            this.ui = ui;
            this.isFF = isFF;
            startTimeNano = System.nanoTime();
            initialDisplayTimeMs = currentTotalPositionMs;

            timer = new FFRewindTimer(handler, currentTotalPositionMs, maxTotalPositionMs);
            timer.addObserver(timerObserver);
            timer.addObserver(this);
        }

        void start() {
            setSpeedLevel(0);
            timer.run();
        }

        void stop() {
            ui.onFFRewindSpeed(PlaybackUi.SpeedLevel.STOP);
            timer.removeObserver(this);
            timer.stop();
        }

        @Override
        public void onTimerUpdated(long displayTimeMs) {
            long skippedMs = Math.abs(displayTimeMs - initialDisplayTimeMs);
            if (skippedMs > SPEED_LEVEL_THRESHOLDS[currentSpeedLevelIndex])
                setSpeedLevel(currentSpeedLevelIndex + 1);
        }

        @Override
        public void onTimerLimitReached() {
            ui.onFFRewindSpeed(PlaybackUi.SpeedLevel.STOP);
        }

        long getDisplayTimeMs() {
            return timer.getDisplayTimeMs();
        }

        long getRewindWallTimeMs() {
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNano);
        }

        private void setSpeedLevel(int speedLevelIndex) {
            if (speedLevelIndex != currentSpeedLevelIndex) {
                currentSpeedLevelIndex = speedLevelIndex;
                int speed = SPEED_LEVEL_SPEEDS[speedLevelIndex];
                timer.changeSpeed(isFF ? speed : -speed);

                ui.onFFRewindSpeed(SPEED_LEVELS[speedLevelIndex]);
            }
        }
    }
}
