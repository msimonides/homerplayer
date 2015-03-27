package com.studio4plus.audiobookplayer.service;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.studio4plus.audiobookplayer.AudioBookPlayerApplication;
import com.studio4plus.audiobookplayer.model.AudioBook;
import com.studio4plus.audiobookplayer.model.Position;

import java.io.File;
import java.io.IOException;

public class AudioBookPlayer extends Handler {

    public interface Observer {
        public void onPlaybackStopped();
    }

    private static final int MSG_CONTROL_FILE_COMPLETE = 1;
    private static final int MSG_CONTROL_STOPPED = 2;

    private final Handler playbackThreadHandler;
    private final Observer observer;

    private AudioBook audioBook;

    public AudioBookPlayer(Observer observer, AudioBook book) {
        super(Looper.getMainLooper());
        this.observer = observer;
        this.audioBook = book;
        HandlerThread thread = new HandlerThread("Playback");
        thread.start();
        playbackThreadHandler = new PlaybackHandler(thread.getLooper(), this);
    }

    public void startPlayback() {
        Message message = playbackThreadHandler.obtainMessage(PlaybackHandler.MSG_PLAYBACK_START);
        message.obj = new BookPlaybackInfo(audioBook);
        playbackThreadHandler.sendMessage(message);
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
                    startPlayback();
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

    private static class BookPlaybackInfo {
        public final File directoryPath;
        public final Position position;

        private BookPlaybackInfo(AudioBook audioBook) {
            this.directoryPath =
                    AudioBookPlayerApplication.getAudioBookManager().getAbsolutePath(audioBook);
            this.position = audioBook.getLastPosition();
        }
    }

    private static class PlaybackHandler extends Handler implements MediaPlayer.OnCompletionListener  {

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
                    BookPlaybackInfo playbackInfo = (BookPlaybackInfo) message.obj;
                    startPlayback(playbackInfo.directoryPath, playbackInfo.position);
                    break;
                case MSG_PLAYBACK_STOP:
                    stopPlayback();
                    break;
            }
        }

        private void startPlayback(final File directoryPath, final Position lastPosition) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(this);

            File currentFile = new File(directoryPath, lastPosition.filePath);
            try {
                mediaPlayer.setDataSource(currentFile.getAbsolutePath());
                mediaPlayer.prepare();
                mediaPlayer.seekTo(lastPosition.seekPosition);
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
