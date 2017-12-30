package com.studio4plus.homerplayer.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.studio4plus.homerplayer.GlobalSettings;

public class OrientationActivityDelegate
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final Activity activity;
    private final GlobalSettings globalSettings;

    OrientationActivityDelegate(@NonNull Activity activity, GlobalSettings globalSettings) {
        this.activity = activity;
        this.globalSettings = globalSettings;
    }

    public void onStart() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity);
        updateOrientation();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public void onStop() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateOrientation();
    }

    private void updateOrientation() {
        activity.setRequestedOrientation(globalSettings.getScreenOrientation());
    }
}
