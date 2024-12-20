package com.studio4plus.homerplayer.service;

import android.app.Notification;
import android.content.Context;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import androidx.annotation.NonNull;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.content.ContextCompat;
import androidx.media.AudioAttributesCompat;
import androidx.media.AudioFocusRequestCompat;
import androidx.media.AudioManagerCompat;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.events.PlaybackFatalErrorEvent;
import com.studio4plus.homerplayer.events.PlaybackProgressedEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppedEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppingEvent;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.player.DurationQueryController;
import com.studio4plus.homerplayer.player.PlaybackController;
import com.studio4plus.homerplayer.player.Player;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

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

    @Inject public AudioBookManager audioBookManager;
    @Inject public GlobalSettings globalSettings;
    @Inject public EventBus eventBus;

    private Player player;
    private DurationQuery durationQueryInProgress;
    private AudioBookPlayback playbackInProgress;
    private DeviceMotionDetector motionDetector;
    private Handler handler;
    private final SleepFadeOut sleepFadeOut = new SleepFadeOut();
    private final AudioFocusRequestCompat audioFocusRequest = createAudioFocusRequest();
    private MediaSessionCompat mediaSession;

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
        mediaSession = new MediaSessionCompat(this, "playback");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPause() {
                onStop();
            }

            @Override
            public void onStop() {
                stopPlayback();
            }
        });
    }

    @Override
    public void onDestroy() {
        Timber.i("PlaybackService.onDestroy");
        stopPlayback();
        mediaSession = null;
        super.onDestroy();
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
                android.R.drawable.ic_media_play
        ).setStyle(
                new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
        ).build();
        ContextCompat.startForegroundService(
                this, new Intent(this, PlaybackService.class));
        startForeground(NOTIFICATION_ID, notification);

        if (book.getTotalDurationMs() == AudioBook.UNKNOWN_POSITION) {
            Timber.i("PlaybackService.startPlayback: create DurationQuery");
            durationQueryInProgress = new DurationQuery(player, book);
        } else {
            Timber.i("PlaybackService.startPlayback: create AudioBookPlayback");
            playbackInProgress = new AudioBookPlayback(
                    player,
                    mediaSession,
                    handler,
                    book,
                    globalSettings.getJumpBackPreferenceMs(),
                    globalSettings.getPlaybackSpeed());
        }
    }

    public State getState() {
        if (durationQueryInProgress != null) {
            return State.PREPARATION;
        } else if (playbackInProgress != null) {
            return State.PLAYBACK;
        } else {
            return State.IDLE;
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

        Timber.i("PlaybackService.stopPlayback");
        onPlaybackEnded();
    }

    @Override
    public void onFaceDownStill() {
        Timber.i("PlaybackService.onFaceDownStill");
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
            Timber.i("PlaybackService.onAudioFocusChange");
            stopPlayback();
        }
    }

    public class ServiceBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    private void restartPlayback() {
        startPlayback(audioBookManager.getCurrentBook());
    }

    private void onPlaybackEnded() {
        durationQueryInProgress = null;
        playbackInProgress = null;
        if (motionDetector != null)
             motionDetector.disable();
        mediaSession.setActive(false);

        stopSleepTimer();
        dropAudioFocus();
        eventBus.post(PLAYBACK_STOPPING_EVENT);
    }

    private void onPlayerReleased() {
        Timber.i("PlaybackService.onPlayerReleased");
        if (playbackInProgress != null || durationQueryInProgress != null) {
            onPlaybackEnded();
        }
        player = null;
        eventBus.post(PLAYBACK_STOPPED_EVENT);
        stopForeground(true);
        stopSelf();
    }

    private void requestAudioFocus() {
        AudioManager audioManager =
                (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        AudioManagerCompat.requestAudioFocus(audioManager, audioFocusRequest);
    }

    private void dropAudioFocus() {
        AudioManager audioManager =
                (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest);
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

    private AudioFocusRequestCompat createAudioFocusRequest() {
        AudioAttributesCompat attributes = new AudioAttributesCompat.Builder()
                .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                .build();
        return new AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(this)
                .setAudioAttributes(attributes)
                .build();
    }

    private class AudioBookPlayback implements PlaybackController.Observer {

        final @NonNull AudioBook audioBook;
        private final @NonNull PlaybackController controller;
        private final @NonNull Handler handler;
        private final @NonNull MediaSessionCompat mediaSession;
        private final float playbackSpeed;
        private @NonNull PlaybackStateCompat mediaSessionState;
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
                @NonNull MediaSessionCompat mediaSession,
                @NonNull Handler handler,
                @NonNull AudioBook audioBook,
                int jumpBackMs,
                float playbackSpeed
        ) {
            this.audioBook = audioBook;
            this.handler = handler;
            this.mediaSession = mediaSession;
            this.playbackSpeed = playbackSpeed;

            controller = player.createPlayback();
            controller.setObserver(this);
            AudioBook.Position position = audioBook.getLastPosition();
            long startPositionMs = Math.max(0, position.seekPosition - jumpBackMs);
            controller.start(position.uri, startPositionMs);
            mediaSessionState = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_BUFFERING, getCurrentTotalPositionMs(), playbackSpeed)
                    .setActions(PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_STOP)
                    .build();
            mediaSession.setPlaybackState(mediaSessionState);
            mediaSession.setMetadata(
                    new MediaMetadataCompat.Builder()
                            .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, audioBook.getTitle())
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, audioBook.getTotalDurationMs())
                            .build());
            mediaSession.setActive(true);
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
            controller.start(position.uri, position.seekPosition);
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
            mediaSessionState = new PlaybackStateCompat.Builder(mediaSessionState)
                    .setState(PlaybackStateCompat.STATE_PLAYING, getCurrentTotalPositionMs(), playbackSpeed)
                    .build();
            mediaSession.setPlaybackState(mediaSessionState);
        }

        @Override
        public void onDuration(Uri uri, long durationMs) {
            audioBook.offerFileDuration(uri, durationMs);
        }

        @Override
        public void onPlaybackEnded() {
            boolean hasMoreToPlay = audioBook.advanceFile();
            Timber.i("PlaybackService.AudioBookPlayback.onPlaybackEnded: %s",
                    (hasMoreToPlay ? "more to play" : "finished"));
            if (hasMoreToPlay) {
                AudioBook.Position position = audioBook.getLastPosition();
                controller.start(position.uri, position.seekPosition);
            } else {
                PlaybackService.this.onPlaybackEnded();
                controller.release();
            }
        }

        @Override
        public void onPlaybackStopped(long currentPositionMs) {
            audioBook.updatePosition(currentPositionMs);
        }

        @Override
        public void onPlaybackError(Uri uri) {
            eventBus.post(new PlaybackFatalErrorEvent(uri));
        }

        @Override
        public void onPlayerReleased() {
            handler.removeCallbacks(updatePosition);
            PlaybackService.this.onPlayerReleased();
        }
    }

    private class DurationQuery implements DurationQueryController.Observer {

        private final AudioBook audioBook;
        private final DurationQueryController controller;

        private DurationQuery(Player player, AudioBook audioBook) {
            this.audioBook = audioBook;

            List<Uri> uris = audioBook.getFilesWithNoDuration();
            controller = player.createDurationQuery(uris);
            controller.start(this);
            mediaSession.setMetadata(
                    new MediaMetadataCompat.Builder()
                            .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, audioBook.getTitle())
                            .build());
            mediaSession.setActive(true);
        }

        public void stop() {
            controller.stop();
        }

        @Override
        public void onDuration(Uri uri, long durationMs) {
            audioBook.offerFileDuration(uri, durationMs);
        }

        @Override
        public void onFinished() {
            Timber.i("PlaybackService.DurationQuery.onFinished");
            Preconditions.checkState(durationQueryInProgress == this);
            durationQueryInProgress = null;
            playbackInProgress = new AudioBookPlayback(
                    player,
                    mediaSession,
                    handler,
                    audioBook,
                    globalSettings.getJumpBackPreferenceMs(),
                    globalSettings.getPlaybackSpeed());
        }

        @Override
        public void onPlayerReleased() {
            PlaybackService.this.onPlayerReleased();
        }

        @Override
        public void onPlayerError(Uri uri) {
            eventBus.post(new PlaybackFatalErrorEvent(uri));
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
            if (currentVolume <= 0) {
                Timber.i("SleepFadeOut stop");
                stopPlayback();
            } else {
                handler.postDelayed(this, STEP_INTERVAL_MS);
            }
        }
    }
}
