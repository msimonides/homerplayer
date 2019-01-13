package com.studio4plus.homerplayer.events;

import androidx.annotation.Nullable;

public class DemoSamplesInstallationFinishedEvent {

    public final boolean success;
    public final @Nullable String errorMessage;

    public DemoSamplesInstallationFinishedEvent(boolean success, @Nullable String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }
}
