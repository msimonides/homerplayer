package com.studio4plus.homerplayer.analytics;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Map;

public class StatsLogger {

    @NonNull
    private final FirebaseAnalytics firebaseAnalytics;

    public StatsLogger(@NonNull Context context) {
        this.firebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    public void logEvent(@NonNull String eventName) {
        firebaseAnalytics.logEvent(eventName, null);
    }

    public void logEvent(@NonNull String eventName, @NonNull Map<String, String> eventData) {
        Bundle params = new Bundle();
        for (Map.Entry<String, String> entry : eventData.entrySet()) {
            params.putString(entry.getKey(), entry.getValue());
        }
        firebaseAnalytics.logEvent(eventName, params);
    }
}
