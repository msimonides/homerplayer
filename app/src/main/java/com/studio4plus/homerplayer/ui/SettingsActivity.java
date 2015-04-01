package com.studio4plus.homerplayer.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.widget.Toast;

import com.studio4plus.homerplayer.R;

public class SettingsActivity extends Activity {

    // TODO: figure out if these constants can somehow be shared with the keys in preferences.xml
    public static final String KEY_KIOSK_MODE = "kiosk_mode_preference";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }


    public static class SettingsFragment
            extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);

            if (Build.VERSION.SDK_INT < 21) {
                Preference kioskModePreference = findPreference("kiosk_mode_preference");
                kioskModePreference.setEnabled(false);
                kioskModePreference.setSummary(R.string.pref_kiosk_mode_summary_old_version);
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onStop() {
            super.onStop();
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        @SuppressLint("CommitPrefEdits")
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(KEY_KIOSK_MODE)) {
                boolean isTaskLocked = ApplicationLocker.isTaskLocked(getActivity());
                boolean newKioskModeEnabled = sharedPreferences.getBoolean(KEY_KIOSK_MODE, false);
                if (newKioskModeEnabled && !isTaskLocked) {
                    boolean isLocked = ApplicationLocker.lockApplication(getActivity());
                    if (!isLocked) {
                        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                                .setMessage(getResources().getString(
                                        R.string.settings_device_owner_required_alert))
                                .setNeutralButton(android.R.string.ok, null)
                                .create();
                        dialog.show();
                        sharedPreferences.edit().putBoolean(KEY_KIOSK_MODE, false).commit();
                        SwitchPreference switchPreference =
                                (SwitchPreference) findPreference(KEY_KIOSK_MODE);
                        switchPreference.setChecked(false);
                    }
                } else if (!newKioskModeEnabled && isTaskLocked) {
                    ApplicationLocker.unlockApplication(getActivity());
                }
            }
        }
    }
}
