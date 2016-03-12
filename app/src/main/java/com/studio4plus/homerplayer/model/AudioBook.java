package com.studio4plus.homerplayer.model;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.util.DebugUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AudioBook {

    public final static long UNKNOWN_POSITION = -1;

    public interface UpdateObserver {
        void onAudioBookStateUpdated(AudioBook audioBook);
    }

    private final FileSet fileSet;
    private List<Long> fileDurations;
    private ColourScheme colourScheme;
    private Position lastPosition;

    private UpdateObserver updateObserver;

    public AudioBook(FileSet fileSet) {
        this.fileSet = fileSet;
        this.lastPosition = new Position(fileSet.filePaths.get(0), 0);
        this.fileDurations = new ArrayList<>(fileSet.filePaths.size());
    }

    public void setUpdateObserver(UpdateObserver updateObserver) {
        this.updateObserver = updateObserver;
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
            notifyUpdateObserver();
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

    public void updatePosition(long seekPosition) {
        DebugUtil.verifyIsOnMainThread();
        lastPosition = new Position(lastPosition.filePath, seekPosition);
        notifyUpdateObserver();
    }

    public void resetPosition() {
        DebugUtil.verifyIsOnMainThread();
        lastPosition = new Position(fileSet.filePaths.get(0), 0);
        notifyUpdateObserver();
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
            notifyUpdateObserver();
        }

        return hasMoreFiles;
    }

    List<Long> getFileDurations() {
        return fileDurations;
    }

    void restore(ColourScheme colourScheme, Position lastPosition, List<Long> fileDurations) {
        this.lastPosition = lastPosition;
        if (colourScheme != null)
            this.colourScheme = colourScheme;
        if (fileDurations != null)
            this.fileDurations = fileDurations;
    }

    private static String directoryToTitle(String directory) {
        return directory.replace('_', ' ');
    }

    private void notifyUpdateObserver() {
        if (updateObserver != null)
            updateObserver.onAudioBookStateUpdated(this);
    }
}
