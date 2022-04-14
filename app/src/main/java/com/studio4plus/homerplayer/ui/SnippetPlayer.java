package com.studio4plus.homerplayer.ui;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.studio4plus.homerplayer.crashreporting.CrashReporting;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.player.PlaybackController;
import com.studio4plus.homerplayer.player.Player;

import java.io.File;

import de.greenrobot.event.EventBus;

/**
 * Plays the current audiobook for a short amount of time. Just to demonstrate.
 */
public class SnippetPlayer implements PlaybackController.Observer {

    private static final long PLAYBACK_TIME_MS = 5000;

    private static final String TAG = "SnippetPlayer";
    final private PlaybackController playbackController;
    private long startPositionMs = -1;
    private boolean isPlaying = false;

    public SnippetPlayer(Context context, EventBus eventBus, float playbackSpeed) {
        Player player = new Player(context, eventBus);
        player.setPlaybackSpeed(playbackSpeed);
        playbackController = player.createPlayback();
        playbackController.setObserver(this);
    }

    public void play(AudioBook audioBook) {
        AudioBook.Position position = audioBook.getLastPosition();

        isPlaying = true;
        playbackController.start(position.uri, position.seekPosition);
    }

    public void stop() {
        playbackController.stop();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    @Override
    public void onDuration(Uri uri, long durationMs) {}

    @Override
    public void onPlaybackProgressed(long currentPositionMs) {
        if (startPositionMs < 0) {
            startPositionMs = currentPositionMs;
        } else {
            if (currentPositionMs - startPositionMs > PLAYBACK_TIME_MS) {
                playbackController.stop();
            }
        }
    }

    @Override
    public void onPlaybackEnded() {}

    @Override
    public void onPlaybackStopped(long currentPositionMs) {}

    @Override
    public void onPlaybackError(Uri uri) {
        CrashReporting.log(Log.INFO, TAG,"Unable to play snippet: " + uri.toString());
    }

    @Override
    public void onPlayerReleased() {
        isPlaying = false;
    }
}
