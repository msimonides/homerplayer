package com.studio4plus.homerplayer.player;

import android.net.Uri;

public interface PlaybackController {

    interface Observer {
        void onDuration(Uri uri, long durationMs);

        /**
         * Playback position progressed. Called more or less once per second of playback in media
         * time (i.e. affected by the playback speed).
         */
        void onPlaybackProgressed(long currentPositionMs);

        /**
         * Playback ended because it reached the end of track
         */
        void onPlaybackEnded();

        /**
         * Playback stopped on request.
         */
        void onPlaybackStopped(long currentPositionMs);

        /**
         * Error playing file.
         */
        void onPlaybackError(Uri uri);

        /**
         * The player has been released.
         */
        void onPlayerReleased();
    }

    void setObserver(Observer observer);
    void start(Uri uri, long positionPosition);
    void pause();
    void stop();
    void release();
    long getCurrentPosition();
}
