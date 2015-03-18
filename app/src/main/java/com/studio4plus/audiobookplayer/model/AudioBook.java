package com.studio4plus.audiobookplayer.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioBook {

    public interface PositionObserver {
        public void onAudioBookPositionChanged(AudioBook audioBook);
    }

    private final String id;
    private final String directoryName;
    private final List<String> filePaths;
    private Position lastPosition;

    private PositionObserver positionObserver;

    public AudioBook(String id, String directoryName, String[] filePaths, Position lastPosition) {
        this.id = id;
        this.directoryName = directoryName;
        this.filePaths = new ArrayList<>(filePaths.length);
        this.filePaths.addAll(Arrays.asList(filePaths));

        if (lastPosition != null)
            this.lastPosition = lastPosition;
        else
            this.lastPosition = new Position(filePaths[0], 0);
    }

    public void setPositionObserver(PositionObserver positionObserver) {
        this.positionObserver = positionObserver;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public String getTitle() {
        return directoryName;
    }

    public String getId() {
        return id;
    }

    public Position getLastPosition() {
        return lastPosition;
    }

    public void updatePosition(int seekPosition) {
        lastPosition = new Position(lastPosition.filePath, seekPosition);
        notifyPositionObserver();
    }

    public void resetPosition() {
        lastPosition = new Position(filePaths.get(0), 0);
        notifyPositionObserver();
    }

    public boolean advanceFile() {
        int newIndex = filePaths.indexOf(lastPosition.filePath) + 1;
        boolean hasMoreFiles = newIndex < filePaths.size();
        if (hasMoreFiles) {
            lastPosition = new Position(filePaths.get(newIndex), 0);
            notifyPositionObserver();
        }

        return hasMoreFiles;
    }

    private void notifyPositionObserver() {
        if (positionObserver != null)
            positionObserver.onAudioBookPositionChanged(this);
    }
}
