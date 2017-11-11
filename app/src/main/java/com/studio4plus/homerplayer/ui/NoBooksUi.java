package com.studio4plus.homerplayer.ui;

import android.support.annotation.NonNull;

public interface NoBooksUi {

    interface InstallProgressObserver {
        void onDownloadProgress(int transferredBytes, int totalBytes);
        void onInstallStarted();
        void onFailure();
    }

    void initWithController(@NonNull UiControllerNoBooks controller);
    @NonNull InstallProgressObserver showInstallProgress(boolean isAlreadyInstalling);
    void shutdown();
}
