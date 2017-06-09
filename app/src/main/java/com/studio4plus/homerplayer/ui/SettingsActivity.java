package com.studio4plus.homerplayer.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.BuildConfig;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.HomerPlayerDeviceAdmin;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.events.DeviceAdminChangeEvent;
import com.studio4plus.homerplayer.events.SettingsEnteredEvent;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.model.AudioBookManager;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class SettingsActivity extends BaseActivity {

    // Pseudo preferences that don't change any preference values directly.
    private static final String KEY_FAQ = "faq_preference";
    private static final String KEY_RESET_ALL_BOOK_PROGRESS = "reset_all_book_progress_preference";
    private static final String KEY_UNREGISTER_DEVICE_OWNER = "unregister_device_owner_preference";
    private static final String KEY_VERSION = "version_preference";

    private static final String FAQ_URL = "https://goo.gl/1RVxFW";

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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    public static class SettingsFragment
            extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Inject public AudioBookManager audioBookManager;
        @Inject public GlobalSettings globalSettings;

        private SnippetPlayer snippetPlayer = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            HomerPlayerApplication.getComponent(getActivity()).inject(this);

            addPreferencesFromResource(R.xml.preferences);

            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            updateScreenOrientationSummary(sharedPreferences);
            updatePlaybackSpeedSummary(sharedPreferences);
            updateJumpBackSummary(sharedPreferences);

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

                updateUnregisterDeviceOwner(HomerPlayerDeviceAdmin.isDeviceOwner(getActivity()));
            } else {
                getPreferenceScreen().removePreference(preferenceUnregisterDeviceOwner);
            }

            ConfirmDialogPreference preferenceResetProgress =
                    (ConfirmDialogPreference) findPreference(KEY_RESET_ALL_BOOK_PROGRESS);
            preferenceResetProgress.setOnConfirmListener(new ConfirmDialogPreference.OnConfirmListener() {
                @Override
                public void onConfirmed() {
                    audioBookManager.resetAllBookProgress();
                    Toast.makeText(
                            getActivity(),
                            R.string.pref_reset_all_book_progress_done,
                            Toast.LENGTH_SHORT).show();
                }
            });

            setupFaq();
            updateVersionSummary();
        }

        @Override
        public void onStart() {
            super.onStart();
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            EventBus.getDefault().register(this);

            // A fix for the action bar covering the first preference.
            Preconditions.checkNotNull(getView());
            getView().setFitsSystemWindows(true);
        }

        @Override
        public void onPause() {
            super.onPause();
            if (snippetPlayer != null) {
                snippetPlayer.stop();
                snippetPlayer = null;
            }
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
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            if (GlobalSettings.KEY_PLAYBACK_SPEED.equals(preference.getKey()) &&
                    (snippetPlayer == null || !snippetPlayer.isPlaying())) {
                playSnippet();
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case GlobalSettings.KEY_KIOSK_MODE:
                    onKioskModeSwitched(sharedPreferences);
                    break;
                case GlobalSettings.KEY_SIMPLE_KIOSK_MODE:
                    onAnyKioskModeSwitched(sharedPreferences.getBoolean(key, false));
                    break;
                case GlobalSettings.KEY_JUMP_BACK:
                    updateJumpBackSummary(sharedPreferences);
                    break;
                case GlobalSettings.KEY_SCREEN_ORIENTATION:
                    updateScreenOrientationSummary(sharedPreferences);
                    break;
                case GlobalSettings.KEY_PLAYBACK_SPEED:
                    updatePlaybackSpeedSummary(sharedPreferences);
                    playSnippet();
                    break;
            }
        }

        @SuppressWarnings("UnusedDeclaration")
        public void onEvent(DeviceAdminChangeEvent deviceAdminChangeEvent) {
            updateUnregisterDeviceOwner(deviceAdminChangeEvent.isEnabled);
        }

        private void updateListPreferenceSummary(SharedPreferences sharedPreferences,
                                                 String key,
                                                 int default_value_res_id) {
            String stringValue = sharedPreferences.getString(key, getString(default_value_res_id));
            ListPreference preference =
                    (ListPreference) findPreference(key);
            int index = preference.findIndexOfValue(stringValue);
            if (index < 0)
                index = 0;
            preference.setSummary(preference.getEntries()[index]);
        }

        private void updateScreenOrientationSummary(SharedPreferences sharedPreferences) {
            updateListPreferenceSummary(
                    sharedPreferences,
                    GlobalSettings.KEY_SCREEN_ORIENTATION,
                    R.string.pref_screen_orientation_default_value);
        }

        private void updatePlaybackSpeedSummary(SharedPreferences sharedPreferences) {
            updateListPreferenceSummary(
                    sharedPreferences,
                    GlobalSettings.KEY_PLAYBACK_SPEED,
                    R.string.pref_playback_speed_default_value);
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

        private void setupFaq() {
            Preference preference = findPreference(KEY_FAQ);
            preference.setSummary(getString(R.string.pref_help_faq_summary, FAQ_URL));
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    openUrl(FAQ_URL);
                    return true;
                }
            });
        }

        private void updateVersionSummary() {
            Preference preference = findPreference(KEY_VERSION);
            preference.setSummary(BuildConfig.VERSION_NAME);
        }

        private void disableDeviceOwner() {
            SwitchPreference kioskModePreference =
                    (SwitchPreference) findPreference(GlobalSettings.KEY_KIOSK_MODE);
            kioskModePreference.setChecked(false);
            HomerPlayerDeviceAdmin.clearDeviceOwner(getActivity());
        }

        private void openUrl(@NonNull String url) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            try {
                startActivity(i);
            }
            catch(ActivityNotFoundException noActivity) {
                Preconditions.checkNotNull(getView());
                Toast.makeText(getView().getContext(),
                        R.string.pref_no_browser_toast, Toast.LENGTH_LONG).show();
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

                    SwitchPreference switchPreference =
                            (SwitchPreference) findPreference(GlobalSettings.KEY_KIOSK_MODE);
                    switchPreference.setChecked(false);
                    // Beware: the code below causes this function to be recursively entered again.
                    // It should be the last thing the function does.
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(GlobalSettings.KEY_KIOSK_MODE, false);
                    editor.commit();
                } else {
                    onAnyKioskModeSwitched(true);
                }
            } else if (!newKioskModeEnabled && isTaskLocked) {
                ApplicationLocker.unlockApplication(getActivity());
                onAnyKioskModeSwitched(false);
            }
        }

        private void onAnyKioskModeSwitched(boolean enable) {
            updateKioskModeSummaries();
            HomeActivity.setEnabled(getActivity(), enable);
            if (enable)
                triggerHomeAppSelectionIfNecessary();
        }

        private void triggerHomeAppSelectionIfNecessary() {
            final Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.addCategory(Intent.CATEGORY_DEFAULT);

            PackageManager pm = getActivity().getPackageManager();
            ResolveInfo resolveInfo = pm.resolveActivity(homeIntent, 0);
            if (resolveInfo.activityInfo.name.equals("com.android.internal.app.ResolverActivity")) {
                getActivity().startActivity(homeIntent);
            }
        }

        private void playSnippet() {
            if (snippetPlayer != null) {
                snippetPlayer.stop();
                snippetPlayer = null;
            }

            AudioBook book = audioBookManager.getCurrentBook();
            if (book != null) {
                snippetPlayer = new SnippetPlayer(getActivity(), globalSettings.getPlaybackSpeed());

                snippetPlayer.play(book);
            }
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
