package com.studio4plus.homerplayer.service;

import java.io.File;

public interface PlaybackController {

    interface Observer {
        void onPlaybackStarted();
        void onDuration(File file, long durationMs);

        /**
         * Playback ended because it reached the end of track
         */
        void onPlaybackEnded();

        /**
         * Playback stopped on request.
         */
        void onPlaybackStopped(long currentPositionMs);

        /**
         * The player has been released.
         */
        void onPlayerReleased();
    }

    void setObserver(Observer observer);
    void start(File file, long positionPosition);
    void pause();
    void stop();
    void release();
    long getCurrentPosition();
}
