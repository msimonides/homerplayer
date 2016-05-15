package com.studio4plus.homerplayer.service;

import android.app.Notification;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.events.PlaybackElapsedTimeSyncEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppedEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppingEvent;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.ui.MainActivity;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class PlaybackService
        extends Service implements FaceDownDetector.Listener {

    private static final String CRASHLYTICS_TAG = "PlaybackService";
    private static final int NOTIFICATION = R.string.playback_service_notification;
    private static final PlaybackStoppingEvent PLAYBACK_STOPPING_EVENT = new PlaybackStoppingEvent();
    private static final PlaybackStoppedEvent PLAYBACK_STOPPED_EVENT = new PlaybackStoppedEvent();

    @Inject public GlobalSettings globalSettings;
    @Inject public EventBus eventBus;

    private Player player;
    private DurationQuery durationQueryInProgress;
    private AudioBookPlayback playbackInProgress;
    private FaceDownDetector faceDownDetector;

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HomerPlayerApplication.getComponent(getApplicationContext()).inject(this);
        // TODO: use Dagger to create FaceDownDetector?
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (FaceDownDetector.hasSensors(sensorManager)) {
            faceDownDetector =
                    new FaceDownDetector(sensorManager, new Handler(getMainLooper()), this);
        }
        Crashlytics.setString(CRASHLYTICS_TAG, "idle");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPlayback();
        Crashlytics.setString(CRASHLYTICS_TAG, "destroyed");
    }

    public void startPlayback(AudioBook book) {
        Preconditions.checkState(playbackInProgress == null);
        Preconditions.checkState(durationQueryInProgress == null);
        Preconditions.checkState(player == null);

        player = HomerPlayerApplication.getComponent(getApplicationContext()).createAudioBookPlayer();

        if (faceDownDetector != null)
            faceDownDetector.enable();

        startForeground(NOTIFICATION, createNotification());

        if (book.getTotalDurationMs() == AudioBook.UNKNOWN_POSITION) {
            durationQueryInProgress = new DurationQuery(player, book);
            Crashlytics.setString(CRASHLYTICS_TAG, "duration query");
        } else {
            playbackInProgress = new AudioBookPlayback(
                    player, book, globalSettings.getJumpBackPreferenceMs());
            Crashlytics.setString(CRASHLYTICS_TAG, "playback");
        }
    }

    public boolean isInPlaybackMode() {
        return player != null;
    }

    public void pauseForRewind() {
        Preconditions.checkNotNull(playbackInProgress);
        playbackInProgress.pauseForRewind();
        Crashlytics.setString(CRASHLYTICS_TAG, "pause for rewind");
    }

    public void resumeFromRewind(long newTotalPositionMs) {
        Preconditions.checkNotNull(playbackInProgress);
        playbackInProgress.resumeFromRewind(newTotalPositionMs);
        Crashlytics.setString(CRASHLYTICS_TAG, "playback");
    }

    public void stopPlayback() {
        if (durationQueryInProgress != null)
            durationQueryInProgress.stop();
        else if (playbackInProgress != null)
            playbackInProgress.stop();

        onPlaybackEnded();
    }

    public void requestElapsedTimeSyncEvent() {
        if (playbackInProgress != null)
            playbackInProgress.requestElapsedTimeSyncEvent();
    }

    @Override
    public void onDeviceFaceDown() {
        stopPlayback();
    }

    public class ServiceBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    private void onPlaybackEnded() {
        durationQueryInProgress = null;
        playbackInProgress = null;
         if (faceDownDetector != null)
            faceDownDetector.disable();

        eventBus.post(PLAYBACK_STOPPING_EVENT);
        Crashlytics.setString(CRASHLYTICS_TAG, "playback stopping");
    }

    private void onPlayerReleased() {
        player = null;
        stopForeground(true);
        eventBus.post(PLAYBACK_STOPPED_EVENT);
        Crashlytics.setString(CRASHLYTICS_TAG, "idle");
    }

    private Notification createNotification() {
        Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(
                getApplicationContext(), 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle(getResources().getString(R.string.playback_service_notification))
                .setContentIntent(intent)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build();
    }

    private class AudioBookPlayback implements PlaybackController.Observer {

        private final AudioBook audioBook;
        private final PlaybackController controller;
        private boolean isPlaying;

        private AudioBookPlayback(Player player, AudioBook audioBook, int jumpBackMs) {
            this.audioBook = audioBook;

            controller = player.createPlayback();
            controller.setObserver(this);
            AudioBook.Position position = audioBook.getLastPosition();
            long startPositionMs = Math.max(0, position.seekPosition - jumpBackMs);
            controller.start(position.file, startPositionMs);
        }

        public void stop() {
            controller.stop();
        }

        public void pauseForRewind() {
            controller.pause();
        }

        public void resumeFromRewind(long newTotalPositionMs) {
            audioBook.updateTotalPosition(newTotalPositionMs);
            AudioBook.Position position = audioBook.getLastPosition();
            controller.start(position.file, position.seekPosition);
        }

        public void requestElapsedTimeSyncEvent() {
            if (isPlaying) {
                eventBus.post(new PlaybackElapsedTimeSyncEvent(
                        audioBook.getLastPositionTime(controller.getCurrentPosition()),
                        audioBook.getTotalDurationMs()));
            }
        }

        @Override
        public void onPlaybackStarted() {
            isPlaying = true;
            long positionTime = audioBook.getLastPositionTime(controller.getCurrentPosition());
            eventBus.post(
                    new PlaybackElapsedTimeSyncEvent(positionTime, audioBook.getTotalDurationMs()));
        }

        @Override
        public void onDuration(File file, long durationMs) {
            audioBook.offerFileDuration(file, durationMs);
        }

        @Override
        public void onPlaybackEnded() {
            isPlaying = false;
            boolean hasMoreToPlay = audioBook.advanceFile();
            if (hasMoreToPlay) {
                AudioBook.Position position = audioBook.getLastPosition();
                controller.start(position.file, position.seekPosition);
            } else {
                audioBook.resetPosition();
                PlaybackService.this.onPlaybackEnded();
                controller.release();
            }
        }

        @Override
        public void onPlaybackStopped(long currentPositionMs) {
            audioBook.updatePosition(currentPositionMs);
        }

        @Override
        public void onPlayerReleased() {
            isPlaying = false;
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
            Preconditions.checkState(durationQueryInProgress == this);
            durationQueryInProgress = null;
            playbackInProgress = new AudioBookPlayback(
                    player, audioBook, globalSettings.getJumpBackPreferenceMs());
            Crashlytics.setString(CRASHLYTICS_TAG, "playback");
        }

        @Override
        public void onPlayerReleased() {
            PlaybackService.this.onPlayerReleased();
        }
    }
}
