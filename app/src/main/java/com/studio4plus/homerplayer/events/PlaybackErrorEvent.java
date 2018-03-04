package com.studio4plus.homerplayer.events;

import java.io.File;

public class PlaybackErrorEvent {
    public final File path;

    public PlaybackErrorEvent(File path) {
        this.path = path;
    }
}
