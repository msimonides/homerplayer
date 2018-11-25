package com.studio4plus.homerplayer.service;

import android.app.Notification;
import android.content.Context;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.events.PlaybackFatalErrorEvent;
import com.studio4plus.homerplayer.events.PlaybackProgressedEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppedEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppingEvent;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.player.DurationQueryController;
import com.studio4plus.homerplayer.player.PlaybackController;
import com.studio4plus.homerplayer.player.Player;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class PlaybackService
        extends Service
        implements DeviceMotionDetector.Listener, AudioManager.OnAudioFocusChangeListener {

    public enum State {
        IDLE,
        PREPARATION,
        PLAYBACK
    }

    private static final long FADE_OUT_DURATION_MS = TimeUnit.SECONDS.toMillis(10);

    private static final int NOTIFICATION_ID = R.string.playback_service_notification;
    private static final PlaybackStoppingEvent PLAYBACK_STOPPING_EVENT = new PlaybackStoppingEvent();
    private static final PlaybackStoppedEvent PLAYBACK_STOPPED_EVENT = new PlaybackStoppedEvent();

    @Inject public GlobalSettings globalSettings;
    @Inject public EventBus eventBus;

    private Player player;
    private DurationQuery durationQueryInProgress;
    private AudioBookPlayback playbackInProgress;
    private DeviceMotionDetector motionDetector;
    private Handler handler;
    private final SleepFadeOut sleepFadeOut = new SleepFadeOut();

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HomerPlayerApplication.getComponent(getApplicationContext()).inject(this);
        // TODO: use Dagger to create DeviceMotionDetector?
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        handler = new Handler(getMainLooper());
        if (sensorManager != null && DeviceMotionDetector.hasSensors(sensorManager)) {
            motionDetector = new DeviceMotionDetector(sensorManager, this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPlayback();
    }

    public void startPlayback(AudioBook book) {
        Preconditions.checkState(playbackInProgress == null);
        Preconditions.checkState(durationQueryInProgress == null);
        Preconditions.checkState(player == null);

        requestAudioFocus();
        player = HomerPlayerApplication.getComponent(getApplicationContext()).createAudioBookPlayer();
        player.setPlaybackSpeed(globalSettings.getPlaybackSpeed());

        if (motionDetector != null)
            motionDetector.enable();

        Notification notification = NotificationUtil.createForegroundServiceNotification(
                getApplicationContext(),
                R.string.playback_service_notification,
                android.R.drawable.ic_media_play);
        startForeground(NOTIFICATION_ID, notification);

        if (book.getTotalDurationMs() == AudioBook.UNKNOWN_POSITION) {
            Crashlytics.log("PlaybackService.startPlayback: create DurationQuery");
            durationQueryInProgress = new DurationQuery(player, book);
        } else {
            Crashlytics.log("PlaybackService.startPlayback: create AudioBookPlayback");
            playbackInProgress = new AudioBookPlayback(
                    player, handler, book, globalSettings.getJumpBackPreferenceMs());
        }
    }

    public State getState() {
        if (player == null) {
            return State.IDLE;
        } else if (durationQueryInProgress != null) {
            return State.PREPARATION;
        } else {
            Preconditions.checkNotNull(playbackInProgress);
            return State.PLAYBACK;
        }
    }

    public void pauseForRewind() {
        Preconditions.checkNotNull(playbackInProgress);
        playbackInProgress.pauseForRewind();
    }

    public void resumeFromRewind() {
        Preconditions.checkNotNull(playbackInProgress);
        playbackInProgress.resumeFromRewind();
    }

    public long getCurrentTotalPositionMs() {
        Preconditions.checkNotNull(playbackInProgress);
        return playbackInProgress.getCurrentTotalPositionMs();
    }

    public AudioBook getAudioBookBeingPlayed() {
        Preconditions.checkNotNull(playbackInProgress);
        return playbackInProgress.audioBook;
    }

    public void stopPlayback() {
        if (durationQueryInProgress != null)
            durationQueryInProgress.stop();
        else if (playbackInProgress != null)
            playbackInProgress.stop();

        Crashlytics.log("PlaybackService.stopPlayback");
        onPlaybackEnded();
    }

    @Override
    public void onFaceDownStill() {
        stopPlayback();
    }

    @Override
    public void onSignificantMotion() {
        resetSleepTimer();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        // TRANSIENT loss is reported on phone calls.
        // Notifications should request TRANSIENT_CAN_DUCK so they won't interfere.
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            stopPlayback();
        }
    }

    public class ServiceBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    private void onPlaybackEnded() {
        durationQueryInProgress = null;
        playbackInProgress = null;
        if (motionDetector != null)
             motionDetector.disable();

        stopSleepTimer();
        dropAudioFocus();
        eventBus.post(PLAYBACK_STOPPING_EVENT);
    }

    private void onPlayerReleased() {
        Crashlytics.log("PlaybackService.onPlayerReleased");
        if (playbackInProgress != null || durationQueryInProgress != null) {
            onPlaybackEnded();
        }
        player = null;
        stopForeground(true);
        eventBus.post(PLAYBACK_STOPPED_EVENT);
    }

    private void requestAudioFocus() {
        AudioManager audioManager =
                (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(
                this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    private void dropAudioFocus() {
        AudioManager audioManager =
                (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
    }

    private void resetSleepTimer() {
        stopSleepTimer();
        long timerMs = globalSettings.getSleepTimerMs();
        if (timerMs > 0)
            sleepFadeOut.scheduleStart(timerMs);
    }

    private void stopSleepTimer() {
        sleepFadeOut.reset();
    }

    private class AudioBookPlayback implements PlaybackController.Observer {

        final @NonNull AudioBook audioBook;
        private final @NonNull PlaybackController controller;
        private final @NonNull Handler handler;
        private final @NonNull Runnable updatePosition = new Runnable() {
            @Override
            public void run() {
                audioBook.updatePosition(controller.getCurrentPosition());
                handler.postDelayed(updatePosition, UPDATE_TIME_MS);
            }
        };

        private final long UPDATE_TIME_MS = TimeUnit.SECONDS.toMillis(10);

        private AudioBookPlayback(
                @NonNull Player player,
                @NonNull Handler handler,
                @NonNull AudioBook audioBook,
                int jumpBackMs) {
            this.audioBook = audioBook;
            this.handler = handler;

            controller = player.createPlayback();
            controller.setObserver(this);
            AudioBook.Position position = audioBook.getLastPosition();
            long startPositionMs = Math.max(0, position.seekPosition - jumpBackMs);
            controller.start(position.file, startPositionMs);
            handler.postDelayed(updatePosition, UPDATE_TIME_MS);
            resetSleepTimer();
        }

        public void stop() {
            controller.stop();
        }

        public void pauseForRewind() {
            handler.removeCallbacks(updatePosition);
            stopSleepTimer();
            controller.pause();
        }

        public void resumeFromRewind() {
            AudioBook.Position position = audioBook.getLastPosition();
            controller.start(position.file, position.seekPosition);
            handler.postDelayed(updatePosition, UPDATE_TIME_MS);
            resetSleepTimer();
        }

        long getCurrentTotalPositionMs() {
            return audioBook.getLastPositionTime(controller.getCurrentPosition());
        }

        @Override
        public void onPlaybackProgressed(long currentPositionMs) {
            eventBus.post(new PlaybackProgressedEvent(
                    audioBook, audioBook.getLastPositionTime(currentPositionMs)));
        }

        @Override
        public void onDuration(File file, long durationMs) {
            audioBook.offerFileDuration(file, durationMs);
        }

        @Override
        public void onPlaybackEnded() {
            boolean hasMoreToPlay = audioBook.advanceFile();
            Crashlytics.log("PlaybackService.AudioBookPlayback.onPlaybackEnded: " +
                    (hasMoreToPlay ? "more to play" : "finished"));
            if (hasMoreToPlay) {
                AudioBook.Position position = audioBook.getLastPosition();
                controller.start(position.file, position.seekPosition);
            } else {
                handler.removeCallbacks(updatePosition);
                audioBook.resetPosition();
                PlaybackService.this.onPlaybackEnded();
                controller.release();
            }
        }

        @Override
        public void onPlaybackStopped(long currentPositionMs) {
            handler.removeCallbacks(updatePosition);
            audioBook.updatePosition(currentPositionMs);
        }

        @Override
        public void onPlaybackError(File path) {
            eventBus.post(new PlaybackFatalErrorEvent(path));
        }

        @Override
        public void onPlayerReleased() {
            PlaybackService.this.onPlayerReleased();
        }
    }

    private class DurationQuery implements DurationQueryController.Observer {

        private final AudioBook audioBook;
        private final DurationQueryController controller;

        private DurationQuery(Player player, AudioBook audioBook) {
            this.audioBook = audioBook;

            List<File> files = audioBook.getFilesWithNoDuration();
            controller = player.createDurationQuery(files);
            controller.start(this);
        }

        public void stop() {
            controller.stop();
        }

        @Override
        public void onDuration(File file, long durationMs) {
            audioBook.offerFileDuration(file, durationMs);
        }

        @Override
        public void onFinished() {
            Crashlytics.log("PlaybackService.DurationQuery.onFinished");
            Preconditions.checkState(durationQueryInProgress == this);
            durationQueryInProgress = null;
            playbackInProgress = new AudioBookPlayback(
                    player, handler, audioBook, globalSettings.getJumpBackPreferenceMs());
        }

        @Override
        public void onPlayerReleased() {
            PlaybackService.this.onPlayerReleased();
        }

        @Override
        public void onPlayerError(File path) {
            eventBus.post(new PlaybackFatalErrorEvent(path));
        }
    }

    private class SleepFadeOut implements Runnable {
        private float currentVolume = 1.0f;
        private final long STEP_INTERVAL_MS = 100;
        private final float VOLUME_DOWN_STEP =  (float) STEP_INTERVAL_MS / FADE_OUT_DURATION_MS;

        public void scheduleStart(long delay) {
            handler.postDelayed(this, delay);
        }

        public void reset() {
            handler.removeCallbacks(this);
            currentVolume = 1.0f;

            // The player may have been released already.
            if (player != null)
              player.setPlaybackVolume(currentVolume);
        }

        @Override
        public void run() {
            currentVolume -= VOLUME_DOWN_STEP;
            player.setPlaybackVolume(currentVolume);
            if (currentVolume <= 0)
                stopPlayback();
            else
                handler.postDelayed(this, STEP_INTERVAL_MS);
        }
    }
}
