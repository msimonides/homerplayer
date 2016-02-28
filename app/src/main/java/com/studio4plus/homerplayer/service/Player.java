package com.studio4plus.homerplayer.service;

import android.net.Uri;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.FileDataSource;
import com.google.common.base.Preconditions;

import java.io.File;

public class Player {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 128;

    private final Allocator exoAllocator;
    private final ExoPlayer exoPlayer;

    public Player() {
        exoPlayer = ExoPlayer.Factory.newInstance(1);
        exoPlayer.setPlayWhenReady(true);  // Call before setting the listener.
        exoAllocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
    }

    private void prepareAudioFile(File file, int startPositionMs) {
        Uri fileUri = Uri.fromFile(file);

        DataSource dataSource = new FileDataSource();
        SampleSource source = new ExtractorSampleSource(
                fileUri, dataSource, exoAllocator, BUFFER_SEGMENT_SIZE * BUFFER_SEGMENT_COUNT);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(
                source, MediaCodecSelector.DEFAULT);
        exoPlayer.seekTo(startPositionMs);
        exoPlayer.prepare(audioRenderer);
    }

    public PlaybackController createPlayback() {
        return new PlaybackControllerImpl();
    }

    private class PlaybackControllerImpl
            extends SimpleExoPlayerListener implements PlaybackController {

        private File currentFile;
        private Observer observer;

        private PlaybackControllerImpl() {
            exoPlayer.addListener(this);
        }

        @Override
        public void setObserver(Observer observer) {
            this.observer = observer;
        }

        @Override
        public void start(File currentFile, int startPositionMs) {
            Preconditions.checkNotNull(observer);
            this.currentFile = currentFile;
            prepareAudioFile(currentFile, startPositionMs);
        }

        public void stop() {
            exoPlayer.stop();
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch(playbackState) {
                case ExoPlayer.STATE_READY:
                    observer.onPlaybackStarted();
                    observer.onDuration(currentFile, exoPlayer.getDuration());
                    break;
                case ExoPlayer.STATE_ENDED:
                    observer.onPlaybackEnded();
                    break;
                case ExoPlayer.STATE_IDLE:
                    long currentPositionMs = exoPlayer.getCurrentPosition();
                    exoPlayer.release();
                    exoPlayer.removeListener(this);
                    observer.onPlayerReleased((int) currentPositionMs);
                    break;
            }
        }
    }
}
