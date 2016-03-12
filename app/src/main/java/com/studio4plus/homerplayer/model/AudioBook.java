package com.studio4plus.homerplayer.model;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.util.DebugUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AudioBook {

    public final static long UNKNOWN_POSITION = -1;

    public interface PositionObserver {
        void onAudioBookPositionChanged(AudioBook audioBook);
    }

    private final FileSet fileSet;
    private final List<Long> fileDurations;
    private ColourScheme colourScheme;
    private Position lastPosition;

    private PositionObserver positionObserver;

    public AudioBook(FileSet fileSet) {
        this.fileSet = fileSet;
        this.lastPosition = new Position(fileSet.filePaths.get(0), 0);
        this.fileDurations = new ArrayList<>(fileSet.filePaths.size());
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

    public long getLastPositionTime() {
        return getLastPositionTime(lastPosition.seekPosition);
    }

    public long getLastPositionTime(long lastFileSeekPosition) {
        int index = fileSet.filePaths.indexOf(lastPosition.filePath);
        Preconditions.checkState(index >= 0);

        if (index <= fileDurations.size()) {
            long totalPosition = 0;
            for (int i = 0; i < index; ++i) {
                totalPosition += fileDurations.get(i);
            }
            return totalPosition + lastFileSeekPosition;
        } else {
            return UNKNOWN_POSITION;
        }
    }

    public void offerFileDuration(String fileName, long durationMs) {
        int index = fileSet.filePaths.indexOf(fileName);
        Preconditions.checkState(index >= 0);
        Preconditions.checkState(index <= fileDurations.size(), "Duration set out of order.");

        // Only set the duration if unknown.
        if (index == fileDurations.size()) {
            fileDurations.add(durationMs);
            // TODO: persist durations.
        }
    }

    public List<String> getFileNamesWithNoDurationUpToPosition() {
        int lastIndex = fileSet.filePaths.indexOf(lastPosition.filePath);
        Preconditions.checkState(lastIndex >= 0);
        int firstIndex = fileDurations.size();
        List<String> fileNames = new ArrayList<>(lastIndex - firstIndex);
        for (int i = firstIndex; i < lastIndex; ++i) {
            fileNames.add(fileSet.filePaths.get(i));
        }
        return fileNames;
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

    public void updatePosition(long seekPosition) {
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
