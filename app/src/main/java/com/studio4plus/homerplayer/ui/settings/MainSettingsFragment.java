package com.studio4plus.homerplayer.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.preference.Preference;
import android.widget.Toast;

import com.studio4plus.homerplayer.BuildConfig;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.model.AudioBookManager;

import javax.inject.Inject;

public class MainSettingsFragment extends BaseSettingsFragment {

    private static final String KEY_RESET_ALL_BOOK_PROGRESS = "reset_all_book_progress_preference";
    private static final String KEY_FAQ = "faq_preference";
    private static final String KEY_VERSION = "version_preference";

    private static final String FAQ_URL = "https://goo.gl/1RVxFW";

    @Inject public AudioBookManager audioBookManager;
    @Inject public GlobalSettings globalSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        HomerPlayerApplication.getComponent(getActivity()).inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey);

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

        SharedPreferences sharedPreferences = getSharedPreferences();
        updateKioskModeSummary();
        updateScreenOrientationSummary(sharedPreferences);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch(key) {
            case GlobalSettings.KEY_SCREEN_ORIENTATION:
                updateScreenOrientationSummary(sharedPreferences);
                break;
            case GlobalSettings.KEY_SIMPLE_KIOSK_MODE:
            case GlobalSettings.KEY_KIOSK_MODE:
                updateKioskModeSummary();
                break;
        }
    }

    private void updateVersionSummary() {
        Preference preference = findPreference(KEY_VERSION);
        preference.setSummary(BuildConfig.VERSION_NAME);
    }

    private void updateScreenOrientationSummary(@NonNull SharedPreferences sharedPreferences) {
        updateListPreferenceSummary(
                sharedPreferences,
                GlobalSettings.KEY_SCREEN_ORIENTATION,
                R.string.pref_screen_orientation_default_value);
    }

    private void updateKioskModeSummary() {
        Preference kioskModeScreen =
                findPreference(GlobalSettings.KEY_KIOSK_MODE_SCREEN);

        int summaryStringId = R.string.pref_kiosk_mode_screen_summary_disabled;
        if (globalSettings.isFullKioskModeEnabled())
            summaryStringId = R.string.pref_kiosk_mode_screen_summary_full;
        else if (globalSettings.isSimpleKioskModeEnabled())
            summaryStringId = R.string.pref_kiosk_mode_screen_summary_simple;
        kioskModeScreen.setSummary(summaryStringId);
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
}
