package com.studio4plus.homerplayer.analytics;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Map;

import javax.inject.Inject;

public class FirebaseStatsLogger implements StatsLogger {

    @NonNull
    private final FirebaseAnalytics firebaseAnalytics;

    @Inject
    public FirebaseStatsLogger(@NonNull FirebaseAnalytics firebaseAnalytics) {
        this.firebaseAnalytics = firebaseAnalytics;
    }

    @Override
    public void logEvent(@NonNull String eventName) {
        firebaseAnalytics.logEvent(eventName, null);
    }

    @Override
    public void logEvent(@NonNull String eventName, @NonNull Map<String, String> eventData) {
        Bundle params = new Bundle();
        for (Map.Entry<String, String> entry : eventData.entrySet()) {
            params.putString(entry.getKey(), entry.getValue());
        }
        firebaseAnalytics.logEvent(eventName, params);
    }
}
