package com.studio4plus.homerplayer.analytics;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Map;

public class StatsLogger {

    private final static String TAG = "StatsLogger";

    public StatsLogger(@NonNull Context ignored) {
    }

    public void logEvent(@NonNull String eventName) {
        Log.i(TAG, "Event: " + eventName);
    }

    public void logEvent(@NonNull String eventName, @NonNull Map<String, String> eventData) {
        Log.i(TAG, "Event: " + eventName + " params: " + eventData);
    }
}
