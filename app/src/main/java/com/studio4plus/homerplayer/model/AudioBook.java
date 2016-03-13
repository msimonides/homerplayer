package com.studio4plus.homerplayer.model;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.util.DebugUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioBook {

    public final static long UNKNOWN_POSITION = -1;

    public interface UpdateObserver {
        void onAudioBookStateUpdated(AudioBook audioBook);
    }

    public class Position {
        public final int fileIndex;
        public final long seekPosition;
        public final File file;

        public Position(int fileIndex, long seekPosition) {
            this.fileIndex = fileIndex;
            this.seekPosition = seekPosition;
            this.file = fileSet.files[fileIndex];
        }
    }

    private final FileSet fileSet;
    private List<Long> fileDurations;
    private ColourScheme colourScheme;
    private Position lastPosition;

    private UpdateObserver updateObserver;

    public AudioBook(FileSet fileSet) {
        this.fileSet = fileSet;
        this.lastPosition = new Position(0, 0);
        this.fileDurations = new ArrayList<>(fileSet.files.length);
    }

    public void setUpdateObserver(UpdateObserver updateObserver) {
        this.updateObserver = updateObserver;
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
        int index = lastPosition.fileIndex;

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

    public void offerFileDuration(File file, long durationMs) {
        int index = Arrays.asList(fileSet.files).indexOf(file);
        Preconditions.checkState(index >= 0);
        Preconditions.checkState(index <= fileDurations.size(), "Duration set out of order.");

        // Only set the duration if unknown.
        if (index == fileDurations.size()) {
            fileDurations.add(durationMs);
            notifyUpdateObserver();
        }
    }

    public List<File> getFilesWithNoDurationUpToPosition() {
        int lastIndex = lastPosition.fileIndex;
        int firstIndex = fileDurations.size();
        List<File> files = new ArrayList<>(lastIndex - firstIndex);
        files.addAll(Arrays.asList(fileSet.files).subList(firstIndex, lastIndex));
        return files;
    }

    public boolean isDemoSample() {
        return fileSet.isDemoSample;
    }

    public void updatePosition(long seekPosition) {
        DebugUtil.verifyIsOnMainThread();
        lastPosition = new Position(lastPosition.fileIndex, seekPosition);
        notifyUpdateObserver();
    }

    public void resetPosition() {
        DebugUtil.verifyIsOnMainThread();
        lastPosition = new Position(0, 0);
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
        int newIndex = lastPosition.fileIndex + 1;
        boolean hasMoreFiles = newIndex < fileSet.files.length;
        if (hasMoreFiles) {
            lastPosition = new Position(newIndex, 0);
            notifyUpdateObserver();
        }

        return hasMoreFiles;
    }

    List<Long> getFileDurations() {
        return fileDurations;
    }

    void restore(
            ColourScheme colourScheme, int fileIndex, long seekPosition, List<Long> fileDurations) {
        this.lastPosition = new Position(fileIndex, seekPosition);
        if (colourScheme != null)
            this.colourScheme = colourScheme;
        if (fileDurations != null)
            this.fileDurations = fileDurations;
    }

    void restoreOldFormat(
            ColourScheme colourScheme, String fileName, long seekPosition, List<Long> fileDurations) {
        if (colourScheme != null)
            this.colourScheme = colourScheme;
        if (fileDurations != null)
            this.fileDurations = fileDurations;

        int fileIndex = -1;
        for (int i = 0; i < fileSet.files.length; ++i) {
            String path = fileSet.files[i].getAbsolutePath();
            if (path.endsWith(fileName)) {
                fileIndex = i;
                break;
            }
        }
        if (fileIndex >= 0) {
            lastPosition = new Position(fileIndex, seekPosition);
        }
    }

    private static String directoryToTitle(String directory) {
        return directory.replace('_', ' ');
    }

    private void notifyUpdateObserver() {
        if (updateObserver != null)
            updateObserver.onAudioBookStateUpdated(this);
    }
}
