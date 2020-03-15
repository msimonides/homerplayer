package com.studio4plus.homerplayer.analytics;

import androidx.annotation.NonNull;

import java.util.Map;

public interface StatsLogger {
    void logEvent(@NonNull String eventName);
    void logEvent(@NonNull String eventName, @NonNull Map<String, String> eventData);
}
