package com.studio4plus.homerplayer.events;

/**
 * Sent to sync elapsed time (e.g. right after playback has started).
 * Contains the current playback position for the audio book being played.
 */
public class PlaybackElapsedTimeSyncEvent {
    public final long playbackPositionMs;
    public final long totalTimeMs;

    public PlaybackElapsedTimeSyncEvent(long playbackPositionMs, long totalTimeMs) {
        this.playbackPositionMs = playbackPositionMs;
        this.totalTimeMs = totalTimeMs;
    }
}
