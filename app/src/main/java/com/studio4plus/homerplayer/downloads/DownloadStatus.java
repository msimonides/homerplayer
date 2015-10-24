package com.studio4plus.homerplayer.downloads;

public class DownloadStatus {
    public final int status;
    public final int transferredBytes;
    public final int totalBytes;

    DownloadStatus(int status, int transferredBytes, int totalBytes) {
        this.status = status;
        this.transferredBytes = transferredBytes;
        this.totalBytes = totalBytes;
    }
}
