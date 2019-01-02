package com.studio4plus.homerplayer.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.events.PlaybackErrorEvent;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import de.greenrobot.event.EventBus;

public class Player {

    private final SimpleExoPlayer exoPlayer;
    private final EventBus eventBus;
    private ExtractorMediaSource.Factory mediaSourceFactory;

    private float playbackSpeed = 1.0f;

    public Player(Context context, EventBus eventBus) {
        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector());
        this.eventBus = eventBus;
    }

    public PlaybackController createPlayback() {
        return new PlaybackControllerImpl(new Handler(Looper.myLooper()));
    }

    public DurationQueryController createDurationQuery(List<File> files) {
        return new DurationQueryControllerImpl(files);
    }

    public void setPlaybackSpeed(float speed) {
        this.playbackSpeed = speed;
        PlaybackParameters params = new PlaybackParameters(speed, 1.0f);
        exoPlayer.setPlaybackParameters(params);
    }

    public void setPlaybackVolume(float volume) {
        exoPlayer.setVolume(volume);
    }

    private void prepareAudioFile(File file, long startPositionMs) {
        Uri fileUri = Uri.fromFile(file);
        MediaSource source = getExtractorMediaSourceFactory().createMediaSource(fileUri);

        exoPlayer.seekTo(startPositionMs);
        exoPlayer.prepare(source, false, true);
    }

    private ExtractorMediaSource.Factory getExtractorMediaSourceFactory() {
        if (mediaSourceFactory == null) {
            DataSource.Factory dataSourceFactory = new FileDataSourceFactory();
            DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
            extractorsFactory.setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING);
            mediaSourceFactory = new ExtractorMediaSource.Factory(dataSourceFactory)
                            .setExtractorsFactory(extractorsFactory);
        }
        return mediaSourceFactory;
    }

    private class PlaybackControllerImpl
            extends com.google.android.exoplayer2.Player.DefaultEventListener
            implements PlaybackController {

        private File currentFile;
        private Observer observer;
        private int lastPlaybackState;
        private Handler handler;
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
        public void start(File currentFile, long startPositionMs) {
            Preconditions.checkNotNull(observer);
            this.currentFile = currentFile;
            exoPlayer.setPlayWhenReady(true);
            prepareAudioFile(currentFile, startPositionMs);
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
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playbackState == lastPlaybackState)
                return;
            lastPlaybackState = playbackState;

            switch(playbackState) {
                case com.google.android.exoplayer2.Player.STATE_READY:
                    observer.onDuration(currentFile, exoPlayer.getDuration());
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
        public void onPlayerError(ExoPlaybackException error) {
            eventBus.post(new PlaybackErrorEvent(
                    error.getMessage(),
                    exoPlayer.getDuration(),
                    exoPlayer.getCurrentPosition(),
                    getFormatDescription()));
            if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                IOException exception = error.getSourceException();
                if (exception instanceof FileDataSource.FileDataSourceException
                        && exception.getCause() instanceof EOFException) {
                    // May happen with files that have seeking or length information.
                    observer.onPlaybackEnded();
                    return;
                }
            }
            observer.onPlaybackError(currentFile);
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

        private String getFormatDescription() {
            Format format = exoPlayer.getAudioFormat();
            if (format != null) {
                return format.toString();
            } else {
                String fileName = currentFile.getName();
                int suffixIndex = fileName.lastIndexOf('.');
                return suffixIndex != -1 ? fileName.substring(suffixIndex) : "";
            }
        }
    }

    private class DurationQueryControllerImpl
            extends com.google.android.exoplayer2.Player.DefaultEventListener
            implements DurationQueryController {

        private final Iterator<File> iterator;
        private File currentFile;
        private Observer observer;
        private boolean releaseOnIdle = false;

        private DurationQueryControllerImpl(List<File> files) {
            Preconditions.checkArgument(!files.isEmpty());
            this.iterator = files.iterator();
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
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch(playbackState) {
                case com.google.android.exoplayer2.Player.STATE_READY:
                    observer.onDuration(currentFile, exoPlayer.getDuration());
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
        public void onPlayerError(ExoPlaybackException error) {
            releaseOnIdle = true;
            observer.onPlayerError(currentFile);
        }

        private boolean processNextFile() {
            boolean hasNext = iterator.hasNext();
            if (hasNext) {
                currentFile = iterator.next();
                prepareAudioFile(currentFile, 0);
            }
            return hasNext;
        }
    }
}
