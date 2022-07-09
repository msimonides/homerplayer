package com.studio4plus.homerplayer.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.events.PlaybackErrorEvent;

import java.util.Iterator;
import java.util.List;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

public class Player {

    private static final String TAG = "Player";

    private final ExoPlayer exoPlayer;
    private final EventBus eventBus;
    private ProgressiveMediaSource.Factory mediaSourceFactory;

    private float playbackSpeed = 1.0f;

    public Player(Context context, EventBus eventBus) {
        exoPlayer = new ExoPlayer.Builder(context).build();
        exoPlayer.addAnalyticsListener(new ExoLogger());
        this.eventBus = eventBus;
    }

    public PlaybackController createPlayback() {
        return new PlaybackControllerImpl(new Handler(Looper.myLooper()));
    }

    public DurationQueryController createDurationQuery(List<Uri> uris) {
        return new DurationQueryControllerImpl(uris);
    }

    public void setPlaybackSpeed(float speed) {
        this.playbackSpeed = speed;
        PlaybackParameters params = new PlaybackParameters(speed, 1.0f);
        exoPlayer.setPlaybackParameters(params);
    }

    public void setPlaybackVolume(float volume) {
        exoPlayer.setVolume(volume);
    }

    private void prepareAudioFile(Uri uri, long startPositionMs) {
        MediaItem mediaItem = MediaItem.fromUri(uri);
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.seekTo(startPositionMs);
        exoPlayer.prepare();
    }

    private class PlaybackControllerImpl implements
            com.google.android.exoplayer2.Player.Listener,
            PlaybackController {

        private Uri currentUri;
        private Observer observer;
        private int lastPlaybackState;
        final private Handler handler;
        private final Runnable updateProgressTask = this::updateProgress;

        private PlaybackControllerImpl(Handler handler) {
            this.handler = handler;
            exoPlayer.setPlayWhenReady(true);  // Call before setting the listener.
            exoPlayer.addListener(this);
            lastPlaybackState = exoPlayer.getPlaybackState();
        }

        @Override
        public void setObserver(Observer observer) {
            this.observer = observer;
        }

        @Override
        public void start(Uri currentUri, long startPositionMs) {
            Preconditions.checkNotNull(observer);
            this.currentUri = currentUri;
            exoPlayer.setPlayWhenReady(true);
            prepareAudioFile(currentUri, startPositionMs);
        }

        @Override
        public void pause() {
            exoPlayer.setPlayWhenReady(false);
            // This ought to be done in onPlayerStateChanged but detecting pause is not as trivial
            // as doing this here directly.
            handler.removeCallbacks(updateProgressTask);
        }

        public void stop() {
            long position = exoPlayer.getCurrentPosition();
            exoPlayer.stop();
            observer.onPlaybackStopped(position);
        }

        @Override
        public void release() {
            exoPlayer.stop();
        }

        @Override
        public long getCurrentPosition() {
            return exoPlayer.getCurrentPosition();
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == lastPlaybackState)
                return;
            lastPlaybackState = playbackState;

            switch(playbackState) {
                case com.google.android.exoplayer2.Player.STATE_READY:
                    observer.onDuration(currentUri, exoPlayer.getDuration());
                    updateProgress();
                    break;
                case com.google.android.exoplayer2.Player.STATE_ENDED:
                    handler.removeCallbacks(updateProgressTask);
                    observer.onPlaybackEnded();
                    break;
                case com.google.android.exoplayer2.Player.STATE_IDLE:
                    handler.removeCallbacks(updateProgressTask);
                    exoPlayer.release();
                    exoPlayer.removeListener(this);
                    observer.onPlayerReleased();
                    break;
            }
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            String audioFormat =
                    exoPlayer.getAudioFormat() != null ? exoPlayer.getAudioFormat().toString() : "unknown";
            Timber.e(error, "Player error. Format %s; uri: %s", audioFormat, currentUri.toString());
            eventBus.post(new PlaybackErrorEvent(
                    error.getMessage(),
                    exoPlayer.getDuration(),
                    exoPlayer.getCurrentPosition(),
                    audioFormat));
            if (error.errorCode == PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE) {
                // May happen with files that have no seeking or length information.
                observer.onPlaybackEnded();
                return;
            }
            observer.onPlaybackError(currentUri);
        }

        private void updateProgress() {
            long positionMs = exoPlayer.getCurrentPosition();
            observer.onPlaybackProgressed(positionMs);

            // Aim a moment after the expected second change. It's necessary because the actual
            // playback speed may be slightly different than playbackSpeed when it's different
            // than 1.0.
            long delayMs = (long) ((1200 - (positionMs % 1000)) * playbackSpeed);
            if (delayMs < 100)
                delayMs += (long) (1000 * playbackSpeed);

            handler.postDelayed(updateProgressTask, delayMs);
        }
    }

    private class DurationQueryControllerImpl implements
            com.google.android.exoplayer2.Player.Listener,
            DurationQueryController {

        private final Iterator<Uri> iterator;
        private Uri uri;
        private Observer observer;
        private boolean releaseOnIdle = false;

        private DurationQueryControllerImpl(List<Uri> uris) {
            Preconditions.checkArgument(!uris.isEmpty());
            this.iterator = uris.iterator();
        }

        @Override
        public void start(Observer observer) {
            this.observer = observer;
            exoPlayer.setPlayWhenReady(false);  // Call before setting the listener.
            exoPlayer.addListener(this);
            processNextFile();
        }

        @Override
        public void stop() {
            releaseOnIdle = true;
            exoPlayer.stop();
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            switch(playbackState) {
                case com.google.android.exoplayer2.Player.STATE_READY:
                    observer.onDuration(uri, exoPlayer.getDuration());
                    boolean hasNext = processNextFile();
                    if (!hasNext)
                        exoPlayer.stop();
                    break;
                case com.google.android.exoplayer2.Player.STATE_IDLE:
                    exoPlayer.removeListener(this);
                    if (releaseOnIdle) {
                        exoPlayer.release();
                        observer.onPlayerReleased();
                    } else {
                        observer.onFinished();
                    }
                    break;
            }
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            String audioFormat =
                    exoPlayer.getAudioFormat() != null ? exoPlayer.getAudioFormat().toString() : "unknown";
            Timber.e(error, "Player error. Format: %s; uri: %s", audioFormat, uri.toString());
            releaseOnIdle = true;
            observer.onPlayerError(uri);
        }

        private boolean processNextFile() {
            boolean hasNext = iterator.hasNext();
            if (hasNext) {
                uri = iterator.next();
                prepareAudioFile(uri, 0);
            }
            return hasNext;
        }
    }
}
