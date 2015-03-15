package com.studio4plus.audiobookplayer.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioBook {

    private final String directoryName;
    private final List<String> filePaths;
    private Position lastPosition;

    public AudioBook(String directoryName, String[] filePaths) {
        this.directoryName = directoryName;
        this.filePaths = new ArrayList<>(filePaths.length);
        this.filePaths.addAll(Arrays.asList(filePaths));
        lastPosition = new Position(filePaths[0], 0);
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public String getTitle() {
        return directoryName;
    }

    public Position getLastPosition() {
        return lastPosition;
    }

    public void updatePosition(int seekPosition) {
        lastPosition = new Position(lastPosition.filePath, seekPosition);
    }

    public void resetPosition() {
        lastPosition = new Position(filePaths.get(0), 0);
    }

    public boolean advanceFile() {
        int newIndex = filePaths.indexOf(lastPosition.filePath) + 1;
        boolean hasMoreFiles = newIndex < filePaths.size();
        if (hasMoreFiles)
            lastPosition = new Position(filePaths.get(newIndex), 0);

        return hasMoreFiles;
    }
}
