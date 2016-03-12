package com.studio4plus.homerplayer.service;

import java.io.File;

public interface PlaybackController {

    interface Observer {
        void onPlaybackStarted();
        void onDuration(File file, long durationMs);
        void onPlaybackEnded();
        void onPlayerReleased(long currentPositionMs);
    }

    void setObserver(Observer observer);
    void start(File file, long positionPosition);
    void stop();
    long getCurrentPosition();
}
