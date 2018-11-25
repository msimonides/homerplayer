package com.studio4plus.homerplayer.events;

import android.support.annotation.NonNull;

public class PlaybackErrorEvent {
    @NonNull
    public final String errorMessage;
    @NonNull
    public final String format;
    public final long durationMs;
    public final long positionMs;

    public PlaybackErrorEvent(@NonNull String errorMessage, long durationMs, long positionMs, @NonNull String format) {
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
        this.positionMs = positionMs;
        this.format = format;
    }
}
