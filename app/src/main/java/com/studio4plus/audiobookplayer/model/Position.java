package com.studio4plus.audiobookplayer.model;

public class Position {
    public final String filePath;
    public final int seekPosition;

    public Position(String filePath, int seekPosition) {
        this.filePath = filePath;
        this.seekPosition = seekPosition;
    }
}
