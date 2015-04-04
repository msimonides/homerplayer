package com.studio4plus.homerplayer.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.R;

public class SettingsActivity extends Activity {

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

            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            updateJumpBackSummary(sharedPreferences);

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

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch(key) {
                case GlobalSettings.KEY_KIOSK_MODE:
                    onKioskModeSwitched(sharedPreferences);
                    break;
                case GlobalSettings.KEY_JUMP_BACK:
                    updateJumpBackSummary(sharedPreferences);
                    break;
            }
        }

        private void updateJumpBackSummary(SharedPreferences sharedPreferences) {
            String stringValue = sharedPreferences.getString(
                    GlobalSettings.KEY_JUMP_BACK, getString(R.string.pref_jump_back_default_value));
            int value = Integer.parseInt(stringValue);
            Preference preference = findPreference(GlobalSettings.KEY_JUMP_BACK);
            if (value == 0) {
                preference.setSummary(R.string.pref_jump_back_entry_disabled);
            } else {
                preference.setSummary(String.format(
                        getString(R.string.pref_jump_back_summary), value));
            }
        }

        @SuppressLint("CommitPrefEdits")
        private void onKioskModeSwitched(SharedPreferences sharedPreferences) {
            boolean isTaskLocked = ApplicationLocker.isTaskLocked(getActivity());
            boolean newKioskModeEnabled =
                    sharedPreferences.getBoolean(GlobalSettings.KEY_KIOSK_MODE, false);
            if (newKioskModeEnabled && !isTaskLocked) {
                boolean isLocked = ApplicationLocker.lockApplication(getActivity());
                if (!isLocked) {
                    AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setMessage(getResources().getString(
                                    R.string.settings_device_owner_required_alert))
                            .setNeutralButton(android.R.string.ok, null)
                            .create();
                    dialog.show();

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(GlobalSettings.KEY_KIOSK_MODE, false);
                    editor.commit();

                    SwitchPreference switchPreference =
                            (SwitchPreference) findPreference(GlobalSettings.KEY_KIOSK_MODE);
                    switchPreference.setChecked(false);
                }
            } else if (!newKioskModeEnabled && isTaskLocked) {
                ApplicationLocker.unlockApplication(getActivity());
            }
        }
    }
}
