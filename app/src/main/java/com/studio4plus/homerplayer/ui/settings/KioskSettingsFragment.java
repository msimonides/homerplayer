package com.studio4plus.homerplayer.ui.settings;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.KioskModeSwitcher;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.deviceadmin.HomerPlayerDeviceAdmin;
import com.studio4plus.homerplayer.events.DeviceAdminChangeEvent;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class KioskSettingsFragment extends BaseSettingsFragment {

    private static final String KEY_UNREGISTER_DEVICE_OWNER = "unregister_device_owner_preference";

    @Inject public KioskModeSwitcher kioskModeSwitcher;
    @Inject public EventBus eventBus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        HomerPlayerApplication.getComponent(getActivity()).inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_kiosk, rootKey);

        if (Build.VERSION.SDK_INT < 21) {
            Preference kioskModePreference = findPreference(GlobalSettings.KEY_KIOSK_MODE);
            kioskModePreference.setEnabled(false);
        }
        if (Build.VERSION.SDK_INT < 19) {
            Preference simpleKioskModePreference =
                    findPreference(GlobalSettings.KEY_SIMPLE_KIOSK_MODE);
            simpleKioskModePreference.setEnabled(false);
        }
        updateKioskModeSummaries();

        ConfirmDialogPreference preferenceUnregisterDeviceOwner =
                (ConfirmDialogPreference) findPreference(KEY_UNREGISTER_DEVICE_OWNER);
        if (Build.VERSION.SDK_INT >= 21) {
            preferenceUnregisterDeviceOwner.setOnConfirmListener(
                    new ConfirmDialogPreference.OnConfirmListener() {
                        @Override
                        public void onConfirmed() {
                            disableDeviceOwner();
                        }
                    });

            updateUnregisterDeviceOwner(HomerPlayerDeviceAdmin.isDeviceOwner(requireActivity()));
        } else {
            getPreferenceScreen().removePreference(preferenceUnregisterDeviceOwner);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        eventBus.register(this);
    }

    @Override
    public void onStop() {
        eventBus.unregister(this);
        super.onStop();
    }

    @Override
    protected int getTitle() {
        return R.string.pref_kiosk_mode_screen_title;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(DeviceAdminChangeEvent deviceAdminChangeEvent) {
        updateUnregisterDeviceOwner(deviceAdminChangeEvent.isEnabled);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case GlobalSettings.KEY_KIOSK_MODE:
                onKioskModeSwitched(sharedPreferences);
                break;
            case GlobalSettings.KEY_SIMPLE_KIOSK_MODE:
                kioskModeSwitcher.onSimpleKioskModeEnabled(
                        requireActivity(), sharedPreferences.getBoolean(key, false));
                updateKioskModeSummaries();
                break;
        }
    }

    private void updateKioskModeSummaries() {
        SwitchPreference fullModePreference =
                (SwitchPreference) findPreference(GlobalSettings.KEY_KIOSK_MODE);
        {
            int summaryStringId;
            if (Build.VERSION.SDK_INT < 21) {
                summaryStringId = R.string.pref_kiosk_mode_full_summary_old_version;
            } else {
                summaryStringId = fullModePreference.isChecked()
                        ? R.string.pref_kiosk_mode_any_summary_on
                        : R.string.pref_kiosk_mode_any_summary_off;
            }
            fullModePreference.setSummary(summaryStringId);
        }

        SwitchPreference simpleModePreference =
                (SwitchPreference) findPreference(GlobalSettings.KEY_SIMPLE_KIOSK_MODE);
        {
            int summaryStringId;
            if (Build.VERSION.SDK_INT < 19) {
                summaryStringId = R.string.pref_kiosk_mode_simple_summary_old_version;
            } else {
                summaryStringId = simpleModePreference.isChecked()
                        ? R.string.pref_kiosk_mode_any_summary_on
                        : R.string.pref_kiosk_mode_any_summary_off;
            }
            simpleModePreference.setSummary(summaryStringId);
            simpleModePreference.setEnabled(!fullModePreference.isChecked());
        }
    }

    private void updateUnregisterDeviceOwner(boolean isEnabled) {
        Preference preference = findPreference(KEY_UNREGISTER_DEVICE_OWNER);
        preference.setEnabled(isEnabled);
        preference.setSummary(getString(isEnabled
                ? R.string.pref_kiosk_mode_unregister_device_owner_summary_on
                : R.string.pref_kiosk_mode_unregister_device_owner_summary_off));
    }

    private void disableDeviceOwner() {
        SwitchPreference kioskModePreference =
                (SwitchPreference) findPreference(GlobalSettings.KEY_KIOSK_MODE);
        kioskModePreference.setChecked(false);
        HomerPlayerDeviceAdmin.clearDeviceOwner(getActivity());
    }

    @SuppressLint("CommitPrefEdits")
    private void onKioskModeSwitched(SharedPreferences sharedPreferences) {
        boolean newKioskModeEnabled =
                sharedPreferences.getBoolean(GlobalSettings.KEY_KIOSK_MODE, false);
        boolean isLockedPermitted = kioskModeSwitcher.isLockTaskPermitted();
        if (newKioskModeEnabled && !isLockedPermitted) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setMessage(getResources().getString(
                            R.string.settings_device_owner_required_alert))
                    .setNeutralButton(android.R.string.ok, null)
                    .create();
            dialog.show();

            SwitchPreference switchPreference =
                    (SwitchPreference) findPreference(GlobalSettings.KEY_KIOSK_MODE);
            switchPreference.setChecked(false);
            // Beware: the code below causes this function to be recursively entered again.
            // It should be the last thing the function does.
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(GlobalSettings.KEY_KIOSK_MODE, false);
            editor.commit();
            return;
        }
        if (isLockedPermitted)
            kioskModeSwitcher.onFullKioskModeEnabled(requireActivity(), newKioskModeEnabled);
        updateKioskModeSummaries();
    }
}
