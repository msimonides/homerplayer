package com.studio4plus.homerplayer.player;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.util.EventLogger;

import timber.log.Timber;

public class ExoLogger extends EventLogger {
    public ExoLogger() {
        super();
    }

    @Override
    protected void logd(String msg) {
        Timber.d(msg);
    }

    @Override
    protected void loge(String msg) {
        Timber.e(msg);
    }
}
