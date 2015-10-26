package com.studio4plus.homerplayer.service;

import android.net.Uri;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.FileDataSource;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.model.Position;

import java.io.File;

import javax.inject.Inject;

public class AudioBookExoPlayer implements ExoPlayer.Listener {

    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 128;

    public interface Observer {
        void onPlaybackStopped();
    }

    private final GlobalSettings globalSettings;
    private final Allocator exoAllocator;

    private AudioBook audioBook;
    private Observer observer;
    private ExoPlayer exoPlayer;

    @Inject
    public AudioBookExoPlayer(GlobalSettings globalSettings) {
        this.globalSettings = globalSettings;
        this.exoAllocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
    }

    public void setObserver(Observer observer) {
        this.observer = observer;
    }

    public void setAudioBook(AudioBook audioBook) {
        this.audioBook = audioBook;
    }

    public void startPlayback() {
        exoPlayer = createExoPlayer();
        startPlayback(globalSettings.getJumpBackPreferenceMs());
    }

    public void stopPlayback() {
        exoPlayer.stop();
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch(playbackState) {
            case ExoPlayer.STATE_ENDED:
                boolean playMore = audioBook.advanceFile();
                if (playMore) {
                    continuePlayback();
                } else {
                    audioBook.resetPosition();
                    stopPlaybackAndReleasePlayer();
                }
                break;
            case ExoPlayer.STATE_IDLE:
                stopPlaybackAndReleasePlayer();
                break;
        }
    }

    @Override
    public void onPlayWhenReadyCommitted() {
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        // The player transitions to the IDLE state after error, so the UI will
        // automatically revert to stop position.
        // Some kind of error reporting would be nice to have (e.g. unsupported format).
    }

    private void startPlayback(int jumpBackOffset) {
        Position position = audioBook.getLastPosition();
        File bookDirectory = audioBook.getAbsoluteDirectory();
        File currentFile = new File(bookDirectory, position.filePath);

        int startPositionMs = Math.max(0, position.seekPosition - jumpBackOffset);
        preparePlayback(exoPlayer, currentFile, startPositionMs);
    }

    private void stopPlaybackAndReleasePlayer() {
        audioBook.updatePosition((int) exoPlayer.getCurrentPosition());
        exoPlayer.release();
        exoPlayer = null;
        observer.onPlaybackStopped();
    }

    private void continuePlayback() {
        startPlayback(0);
    }

    private ExoPlayer createExoPlayer() {
        ExoPlayer exoPlayer = ExoPlayer.Factory.newInstance(1);
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.addListener(this);
        return exoPlayer;
    }

    private void preparePlayback(ExoPlayer exoPlayer, File file, int startPositionMs) {
        Uri fileUri = Uri.fromFile(file);

        DataSource dataSource = new FileDataSource();
        SampleSource source = new ExtractorSampleSource(
                fileUri, dataSource, exoAllocator, BUFFER_SEGMENT_SIZE * BUFFER_SEGMENT_COUNT);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(
                source, MediaCodecSelector.DEFAULT);
        exoPlayer.seekTo(startPositionMs);
        exoPlayer.prepare(audioRenderer);
    }
}
