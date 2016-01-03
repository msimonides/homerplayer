package com.studio4plus.homerplayer.model;

import com.studio4plus.homerplayer.util.DebugUtil;

import java.io.File;
import java.util.List;

public class AudioBook {

    public interface PositionObserver {
        void onAudioBookPositionChanged(AudioBook audioBook);
    }

    private final FileSet fileSet;
    private ColourScheme colourScheme;
    private Position lastPosition;

    private PositionObserver positionObserver;

    public AudioBook(FileSet fileSet) {
        this.fileSet = fileSet;
        this.lastPosition = new Position(fileSet.filePaths.get(0), 0);
    }

    public void setPositionObserver(PositionObserver positionObserver) {
        this.positionObserver = positionObserver;
    }

    public File getAbsoluteDirectory() {
        return fileSet.absolutePath;
    }

    public String getTitle() {
        return directoryToTitle(fileSet.directoryName);
    }

    public String getId() {
        return fileSet.id;
    }

    public Position getLastPosition() {
        return lastPosition;
    }

    public boolean isDemoSample() {
        return fileSet.isDemoSample;
    }

    /**
     * Set the last position.
     * Doesn't call the position observer. Use only when reading AudioBook state from storage.
     */
    void setLastPosition(Position lastPosition) {
        this.lastPosition = lastPosition;
    }

    public void updatePosition(int seekPosition) {
        DebugUtil.verifyIsOnMainThread();
        lastPosition = new Position(lastPosition.filePath, seekPosition);
        notifyPositionObserver();
    }

    public void resetPosition() {
        DebugUtil.verifyIsOnMainThread();
        lastPosition = new Position(fileSet.filePaths.get(0), 0);
        notifyPositionObserver();
    }

    public ColourScheme getColourScheme() {
        return colourScheme;
    }

    public void setColourScheme(ColourScheme colourScheme) {
        this.colourScheme = colourScheme;
    }

    public boolean advanceFile() {
        DebugUtil.verifyIsOnMainThread();
        final List<String> filePaths = fileSet.filePaths;
        int newIndex = filePaths.indexOf(lastPosition.filePath) + 1;
        boolean hasMoreFiles = newIndex < filePaths.size();
        if (hasMoreFiles) {
            lastPosition = new Position(filePaths.get(newIndex), 0);
            notifyPositionObserver();
        }

        return hasMoreFiles;
    }

    private static String directoryToTitle(String directory) {
        return directory.replace('_', ' ');
    }

    private void notifyPositionObserver() {
        if (positionObserver != null)
            positionObserver.onAudioBookPositionChanged(this);
    }
}
