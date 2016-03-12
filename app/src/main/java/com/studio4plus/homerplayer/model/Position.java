package com.studio4plus.homerplayer.model;

public class Position {
    public final String filePath;
    public final long seekPosition;

    public Position(String filePath, long seekPosition) {
        this.filePath = filePath;
        this.seekPosition = seekPosition;
    }
}
