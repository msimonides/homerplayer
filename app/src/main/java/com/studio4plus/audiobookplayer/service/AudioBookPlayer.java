package com.studio4plus.audiobookplayer.service;

import android.media.AudioManager;
import android.media.MediaPlayer;

import com.studio4plus.audiobookplayer.AudioBookPlayerApplication;
import com.studio4plus.audiobookplayer.model.AudioBook;

import java.io.File;
import java.io.IOException;

public class AudioBookPlayer implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private MediaPlayer mediaPlayer;
    private AudioBook audioBook;
    private boolean isPlaying;

    public AudioBookPlayer(AudioBook book) {
        this.audioBook = book;
        this.isPlaying = false;
    }

    public void startPlayback() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);

        File currentFile = new File(
                AudioBookPlayerApplication.getAudioBookManager().getAbsolutePath(audioBook),
                audioBook.getLastPosition().filePath);
        try {
            mediaPlayer.setDataSource(currentFile.getAbsolutePath());
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            // TODO: notify the UI and clean up.
            e.printStackTrace();
        }
        isPlaying = true;
    }

    public void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            isPlaying = false;
            updateAudioBookPosition(mediaPlayer);
            releaseMediaPlayer();
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        releaseMediaPlayer();
        boolean playMore = audioBook.advanceFile();
        if (playMore) {
            startPlayback();
        } else {
            audioBook.resetPosition();
            isPlaying = false;
            // TODO: notify the UI that playback has finished.
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaPlayer.seekTo(audioBook.getLastPosition().seekPosition);
        mediaPlayer.start();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        releaseMediaPlayer();
        // TODO: notify the UI.
        return false;
    }

    private void releaseMediaPlayer() {
        mediaPlayer.release();
        mediaPlayer = null;
    }

    private void updateAudioBookPosition(MediaPlayer mediaPlayer) {
        audioBook.updatePosition(mediaPlayer.getCurrentPosition());
    }
}
