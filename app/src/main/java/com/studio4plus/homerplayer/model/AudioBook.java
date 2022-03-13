package com.studio4plus.homerplayer.model;

import android.net.Uri;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.filescanner.FileSet;
import com.studio4plus.homerplayer.util.DebugUtil;

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
        public final Uri uri;

        public Position(int fileIndex, long seekPosition) {
            this.fileIndex = fileIndex;
            this.seekPosition = seekPosition;
            this.uri = fileSet.uris[fileIndex];
        }
    }

    private final FileSet fileSet;
    private List<Long> fileDurations;
    private ColourScheme colourScheme;
    private Position lastPosition;
    private long totalDuration = UNKNOWN_POSITION;

    private UpdateObserver updateObserver;

    public AudioBook(FileSet fileSet) {
        this.fileSet = fileSet;
        this.lastPosition = new Position(0, 0);
        this.fileDurations = new ArrayList<>(fileSet.uris.length);
    }

    public void setUpdateObserver(UpdateObserver updateObserver) {
        this.updateObserver = updateObserver;
    }

    public String getTitle() {
        return fileSet.name;
    }

    public String getId() {
        return fileSet.id;
    }

    public Position getLastPosition() {
        return lastPosition;
    }

    public long getLastPositionTime(long lastFileSeekPosition) {
        int fullFileCount = lastPosition.fileIndex;

        if (fullFileCount <= fileDurations.size()) {
            return fileDurationSum(fullFileCount) + lastFileSeekPosition;
        } else {
            return UNKNOWN_POSITION;
        }
    }

    public long getTotalDurationMs() {
        return totalDuration;
    }

    public void offerFileDuration(Uri uri, long durationMs) {
        int index = Arrays.asList(fileSet.uris).indexOf(uri);
        Preconditions.checkState(index >= 0);
        Preconditions.checkState(index <= fileDurations.size(), "Duration set out of order.");

        // Only set the duration if unknown.
        if (index == fileDurations.size()) {
            fileDurations.add(durationMs);
            if (fileDurations.size() == fileSet.uris.length)
                totalDuration = fileDurationSum(fileSet.uris.length);
            notifyUpdateObserver();
        }
    }

    public List<Uri> getFilesWithNoDuration() {
        int count = fileSet.uris.length;
        int firstIndex = fileDurations.size();
        List<Uri> files = new ArrayList<>(count - firstIndex);
        files.addAll(Arrays.asList(fileSet.uris).subList(firstIndex, count));
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

    public void updateTotalPosition(long totalPositionMs) {
        Preconditions.checkArgument(totalPositionMs <= totalDuration);

        long fullFileDurationSum = 0;
        int fileIndex = 0;
        for (; fileIndex < fileDurations.size(); ++fileIndex) {
            long total = fullFileDurationSum + fileDurations.get(fileIndex);
            if (totalPositionMs <= total)
                break;
            fullFileDurationSum = total;
        }
        long seekPosition = totalPositionMs - fullFileDurationSum;
        lastPosition = new Position(fileIndex, seekPosition);
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
        boolean hasMoreFiles = newIndex < fileSet.uris.length;
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
        if (fileDurations != null) {
            this.fileDurations = fileDurations;
            if (fileDurations.size() == fileSet.uris.length)
                this.totalDuration = fileDurationSum(fileDurations.size());
        }
    }

    private long fileDurationSum(int fileCount) {
        long totalPosition = 0;
        for (int i = 0; i < fileCount; ++i) {
            totalPosition += fileDurations.get(i);
        }
        return totalPosition;
    }

    private static String directoryToTitle(String directory) {
        return directory.replace('_', ' ');
    }

    private void notifyUpdateObserver() {
        if (updateObserver != null)
            updateObserver.onAudioBookStateUpdated(this);
    }
}
