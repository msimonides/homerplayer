package com.studio4plus.homerplayer.service;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.model.Position;

import java.io.File;
import java.io.IOException;

public class AudioBookPlayer extends Handler {

    public interface Observer {
        public void onPlaybackStopped();
    }

    private static final int MSG_CONTROL_FILE_COMPLETE = 1;
    private static final int MSG_CONTROL_STOPPED = 2;

    private final Context context;
    private final Handler playbackThreadHandler;
    private final Observer observer;

    private AudioBook audioBook;

    public AudioBookPlayer(Context context, Observer observer, AudioBook book) {
        super(Looper.getMainLooper());
        this.context = context;
        this.observer = observer;
        this.audioBook = book;
        HandlerThread thread = new HandlerThread("Playback");
        thread.start();
        playbackThreadHandler = new PlaybackHandler(thread.getLooper(), this);
    }

    public void startPlayback() {
        startPlayback(GlobalSettings.getJumpBackPreferenceMs(context));
    }

    public void stopPlayback() {
        Message message = playbackThreadHandler.obtainMessage(PlaybackHandler.MSG_PLAYBACK_STOP);
        playbackThreadHandler.sendMessage(message);
    }

    @Override
    public void handleMessage(Message message) {
        switch(message.what) {
            case MSG_CONTROL_FILE_COMPLETE:
                boolean playMore = audioBook.advanceFile();
                if (playMore) {
                    continuePlayback();
                } else {
                    audioBook.resetPosition();
                    observer.onPlaybackStopped();
                }
                break;
            case MSG_CONTROL_STOPPED: {
                audioBook.updatePosition(message.arg1);
                observer.onPlaybackStopped();
                break;
            }
        }
    }

    private void continuePlayback() {
        startPlayback(0);
    }

    private void startPlayback(int jumpBackOffset) {
        Message message = playbackThreadHandler.obtainMessage(PlaybackHandler.MSG_PLAYBACK_START);

        Position position = audioBook.getLastPosition();
        File bookDirectory =
                HomerPlayerApplication.getAudioBookManager().getAbsolutePath(audioBook);
        File currentFile = new File(bookDirectory, position.filePath);

        int startPosition = Math.max(0, position.seekPosition - jumpBackOffset);
        message.obj = new PlaybackStartInfo(currentFile.getAbsolutePath(), startPosition);
        playbackThreadHandler.sendMessage(message);
    }

    private static class PlaybackStartInfo {
        public final String filePath;
        public final int startPosition;

        private PlaybackStartInfo(String filePath, int startPosition) {
            this.filePath = filePath;
            this.startPosition = startPosition;
        }
    }

    private static class PlaybackHandler
            extends Handler implements MediaPlayer.OnCompletionListener {

        public static final int MSG_PLAYBACK_START = 1;
        public static final int MSG_PLAYBACK_STOP = 2;

        private final Handler controlHandler;
        private MediaPlayer mediaPlayer;

        private PlaybackHandler(Looper looper, Handler controlHandler) {
            super(looper);
            this.controlHandler = controlHandler;
        }

        @Override
        public void handleMessage(Message message) {
            switch(message.what) {
                case MSG_PLAYBACK_START:
                    PlaybackStartInfo playbackInfo = (PlaybackStartInfo) message.obj;
                    startPlayback(playbackInfo.filePath, playbackInfo.startPosition);
                    break;
                case MSG_PLAYBACK_STOP:
                    stopPlayback();
                    break;
            }
        }

        private void startPlayback(final String filePath, final int startPosition) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(this);

            try {
                mediaPlayer.setDataSource(filePath);
                mediaPlayer.prepare();
                mediaPlayer.seekTo(startPosition);
                mediaPlayer.start();
            } catch (IOException e) {
                // TODO: notify the UI and clean up.
                e.printStackTrace();
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            releaseMediaPlayer();

            Message message = controlHandler.obtainMessage(MSG_CONTROL_FILE_COMPLETE);
            controlHandler.sendMessage(message);
        }

        private void stopPlayback() {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                Message message = controlHandler.obtainMessage(MSG_CONTROL_STOPPED);
                message.arg1 = mediaPlayer.getCurrentPosition();
                controlHandler.sendMessage(message);
                releaseMediaPlayer();
            }
        }

        private void releaseMediaPlayer() {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
