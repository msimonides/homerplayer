package com.studio4plus.homerplayer.analytics;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Map;

public class DummyStatsLogger implements StatsLogger {

    private final static String TAG = "DummyStatsLogger";

    @Override
    public void logEvent(@NonNull String eventName) {
        Log.i(TAG, "Event: " + eventName);
    }

    @Override
    public void logEvent(@NonNull String eventName, @NonNull Map<String, String> eventData) {
        Log.i(TAG, "Event: " + eventName + " params: " + eventData);
    }
}
