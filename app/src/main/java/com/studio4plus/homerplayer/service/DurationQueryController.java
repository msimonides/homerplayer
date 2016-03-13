package com.studio4plus.homerplayer.service;

import java.io.File;

public interface DurationQueryController {

    interface Observer {
        void onDuration(File file, long durationMs);
        void onFinished();
        void onPlayerReleased();
    }

    void start(Observer observer);
    void stop();
}
