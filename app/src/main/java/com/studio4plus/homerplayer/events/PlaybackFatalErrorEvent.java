package com.studio4plus.homerplayer.events;

import android.net.Uri;

public class PlaybackFatalErrorEvent {
    public final Uri uri;

    public PlaybackFatalErrorEvent(Uri uri) {
        this.uri = uri;
    }
}
