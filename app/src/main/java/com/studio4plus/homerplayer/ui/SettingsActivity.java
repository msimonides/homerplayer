package com.studio4plus.homerplayer.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.WindowManager;

import com.studio4plus.homerplayer.BuildConfig;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerDeviceAdmin;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.events.DeviceAdminChangeEvent;
import com.studio4plus.homerplayer.events.SettingsEnteredEvent;

import de.greenrobot.event.EventBus;

public class SettingsActivity extends BaseActivity {

    // Pseudo preferences that don't change any preference values directly.
    private static final String KEY_UNREGISTER_DEVICE_OWNER = "unregister_device_owner_preference";
    private static final String KEY_VERSION = "version_preference";

    private static final int BLOCK_TIME_MS = 500;

    private Handler mainThreadHandler;
    private Runnable unblockEventsTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        mainThreadHandler = new Handler(getMainLooper());
    }

    @Override
    protected void onStart() {
        super.onStart();
        blockEventsOnStart();
        eventBus.post(new SettingsEnteredEvent());
    }

    @Override
    protected void onStop() {
        super.onStop();
        cancelBlockEventOnStart();
    }

    @Override
    protected String getScreenName() {
        return "Settings";
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
            updateScreenOrientationSummary(sharedPreferences);
            updateJumpBackSummary(sharedPreferences);

            if (Build.VERSION.SDK_INT < 21) {
                Preference kioskModePreference = findPreference(GlobalSettings.KEY_KIOSK_MODE);
                kioskModePreference.setEnabled(false);
            }
            updateKioskModeSummary();

            Preference preference = findPreference(KEY_UNREGISTER_DEVICE_OWNER);
            if (Build.VERSION.SDK_INT >= 21) {
                preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        askDisableDeviceOwner();
                        return true;
                    }
                });

                updateUnregisterDeviceOwner(HomerPlayerDeviceAdmin.isDeviceOwner(getActivity()));
            } else {
                getPreferenceScreen().removePreference(preference);
            }

            updateVersionSummary();
        }

        @Override
        public void onStart() {
            super.onStart();
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            EventBus.getDefault().register(this);
        }

        @Override
        public void onStop() {
            super.onStop();
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
            EventBus.getDefault().unregister(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case GlobalSettings.KEY_KIOSK_MODE:
                    onKioskModeSwitched(sharedPreferences);
                    break;
                case GlobalSettings.KEY_JUMP_BACK:
                    updateJumpBackSummary(sharedPreferences);
                    break;
                case GlobalSettings.KEY_SCREEN_ORIENTATION:
                    updateScreenOrientationSummary(sharedPreferences);
                    break;
            }
        }

        @SuppressWarnings("UnusedDeclaration")
        public void onEvent(DeviceAdminChangeEvent deviceAdminChangeEvent) {
            updateUnregisterDeviceOwner(deviceAdminChangeEvent.isEnabled);
        }

        private void updateScreenOrientationSummary(SharedPreferences sharedPreferences) {
            String stringValue = sharedPreferences.getString(
                    GlobalSettings.KEY_SCREEN_ORIENTATION,
                    getString(R.string.pref_screen_orientation_default_value));
            ListPreference preference =
                    (ListPreference) findPreference(GlobalSettings.KEY_SCREEN_ORIENTATION);
            int index = preference.findIndexOfValue(stringValue);
            preference.setSummary(preference.getEntries()[index]);
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

        private void updateKioskModeSummary() {
            SwitchPreference preference =
                    (SwitchPreference) findPreference(GlobalSettings.KEY_KIOSK_MODE);
            int summaryStringId;
            if (Build.VERSION.SDK_INT < 21) {
                summaryStringId = R.string.pref_kiosk_mode_summary_old_version;
            } else {
                summaryStringId = preference.isChecked()
                        ? R.string.pref_kiosk_mode_summary_on
                        : R.string.pref_kiosk_mode_summary_off;
            }
            preference.setSummary(summaryStringId);
        }

        private void updateUnregisterDeviceOwner(boolean isEnabled) {
            Preference preference = findPreference(KEY_UNREGISTER_DEVICE_OWNER);
            preference.setEnabled(isEnabled);
            preference.setSummary(getString(isEnabled
                    ? R.string.pref_unregister_device_owner_summary_on
                    : R.string.pref_unregister_device_owner_summary_off));
        }

        private void updateVersionSummary() {
            Preference preference = findPreference(KEY_VERSION);
            preference.setSummary(BuildConfig.VERSION_NAME);
        }

        private void askDisableDeviceOwner() {
            DialogInterface.OnClickListener disableDeviceOwnerListener =
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            disableDeviceOwner();
                        }
                    };

            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setMessage(getResources().getString(
                            R.string.settings_unregister_device_owner_warning))
                    .setPositiveButton(android.R.string.yes, disableDeviceOwnerListener)
                    .setNegativeButton(android.R.string.no, null)
                    .create();
            dialog.show();
        }

        private void disableDeviceOwner() {
            SwitchPreference kioskModePreference =
                    (SwitchPreference) findPreference(GlobalSettings.KEY_KIOSK_MODE);
            kioskModePreference.setChecked(false);
            HomerPlayerDeviceAdmin.clearDeviceOwner(getActivity());
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
            updateKioskModeSummary();
        }
    }

    private void blockEventsOnStart() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        unblockEventsTask = new Runnable() {
            @Override
            public void run() {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                unblockEventsTask = null;
            }
        };
        mainThreadHandler.postDelayed(unblockEventsTask, BLOCK_TIME_MS);
    }

    private void cancelBlockEventOnStart() {
        if (unblockEventsTask != null)
            mainThreadHandler.removeCallbacks(unblockEventsTask);
    }
}
