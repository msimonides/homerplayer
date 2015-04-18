package com.studio4plus.homerplayer.ui;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;

import com.studio4plus.homerplayer.GlobalSettings;

public class BaseActivity
        extends FragmentActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onStart() {
        super.onStart();
        updateOrientation();
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateOrientation();
    }

    private void updateOrientation() {
        //noinspection ResourceType
        setRequestedOrientation(GlobalSettings.getScreenOrientation(this));
    }
}
