package com.studio4plus.homerplayer.events;

/**
 * Sent to sync elapsed time, right after playback has started and then some time after each full
 * second of playback (in playback time, i.e. affected by playback speed).
 * Contains the current playback position for the audio book being played and the total time.
 */
public class PlaybackProgressedEvent {
    public final long playbackPositionMs;
    public final long totalTimeMs;

    public PlaybackProgressedEvent(long playbackPositionMs, long totalTimeMs) {
        this.playbackPositionMs = playbackPositionMs;
        this.totalTimeMs = totalTimeMs;
    }
}
