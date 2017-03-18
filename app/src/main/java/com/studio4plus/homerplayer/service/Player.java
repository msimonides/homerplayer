package com.studio4plus.homerplayer.service;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.common.base.Preconditions;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class Player {

    private final SimpleExoPlayer exoPlayer;

    public Player(Context context) {
        LoadControl loadControl = new DefaultLoadControl();
        TrackSelector trackSelector = new DefaultTrackSelector();

        exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl);
    }

    public PlaybackController createPlayback() {
        return new PlaybackControllerImpl();
    }

    public DurationQueryController createDurationQuery(List<File> files) {
        return new DurationQueryControllerImpl(files);
    }

    private void prepareAudioFile(File file, long startPositionMs) {
        Uri fileUri = Uri.fromFile(file);

        DataSource.Factory dataSourceFactory = new FileDataSourceFactory();
        MediaSource source = new ExtractorMediaSource(
                fileUri, dataSourceFactory, new DefaultExtractorsFactory(), null, null);

        exoPlayer.seekTo(startPositionMs);
        exoPlayer.prepare(source, false, true);
    }

    private class PlaybackControllerImpl
            extends SimpleExoPlayerEventListener implements PlaybackController {

        private File currentFile;
        private Observer observer;
        private int lastPlaybackState;

        private PlaybackControllerImpl() {
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
        }

        public void stop() {
            exoPlayer.stop();
            observer.onPlaybackStopped(exoPlayer.getCurrentPosition());
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
                case ExoPlayer.STATE_READY:
                    observer.onPlaybackStarted();
                    observer.onDuration(currentFile, exoPlayer.getDuration());
                    break;
                case ExoPlayer.STATE_ENDED:
                    observer.onPlaybackEnded();
                    break;
                case ExoPlayer.STATE_IDLE:
                    exoPlayer.release();
                    exoPlayer.removeListener(this);
                    observer.onPlayerReleased();
                    break;
            }
        }
    }

    private class DurationQueryControllerImpl
            extends SimpleExoPlayerEventListener implements DurationQueryController {

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
                case ExoPlayer.STATE_READY:
                    observer.onDuration(currentFile, exoPlayer.getDuration());
                    boolean hasNext = processNextFile();
                    if (!hasNext)
                        exoPlayer.stop();
                    break;
                case ExoPlayer.STATE_IDLE:
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
