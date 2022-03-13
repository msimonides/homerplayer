package com.studio4plus.homerplayer.player;

import android.net.Uri;

public interface DurationQueryController {

    interface Observer {
        void onDuration(Uri uri, long durationMs);
        void onFinished();
        void onPlayerReleased();
        void onPlayerError(Uri uri);
    }

    void start(Observer observer);
    void stop();
}
