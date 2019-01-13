package com.studio4plus.homerplayer.ui;

import androidx.annotation.NonNull;

import com.studio4plus.homerplayer.concurrency.SimpleFuture;

public interface SpeakerProvider {
    @NonNull SimpleFuture<Speaker> obtainTts();
}
