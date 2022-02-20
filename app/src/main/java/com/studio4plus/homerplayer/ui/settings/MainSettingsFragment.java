package com.studio4plus.homerplayer.ui.settings;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.Preference;

import android.widget.Toast;

import com.studio4plus.homerplayer.BuildConfig;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.model.AudioBookManager;

import java.util.Locale;

import javax.inject.Inject;

public class MainSettingsFragment extends BaseSettingsFragment {

    private static final String KEY_RESET_ALL_BOOK_PROGRESS = "reset_all_book_progress_preference";
    private static final String KEY_AUDIOBOOKS_FOLDER = "audiobooks_folder_preference";
    private static final String KEY_FAQ = "faq_preference";
    private static final String KEY_VERSION = "version_preference";

    private static final String FAQ_URL = "https://goo.gl/1RVxFW";

    @Inject public AudioBookManager audioBookManager;
    @Inject public GlobalSettings globalSettings;

    private final ActivityResultLauncher<Uri> openDocumentTreeContract =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), this::onAudiobooksFolderSet);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        HomerPlayerApplication.getComponent(requireActivity()).inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey);

        ConfirmDialogPreference preferenceResetProgress =
                getPreference(KEY_RESET_ALL_BOOK_PROGRESS);
        preferenceResetProgress.setOnConfirmListener(() -> {
            audioBookManager.resetAllBookProgress();
            Toast.makeText(
                    getActivity(),
                    R.string.pref_reset_all_book_progress_done,
                    Toast.LENGTH_SHORT).show();
        });
        setupAudiobooksFolder();
        setupFaq();
        updateVersionSummary();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateKioskModeSummary();
    }

    @Override
    protected int getTitle() {
        return R.string.settings_title;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch(key) {
            case GlobalSettings.KEY_SIMPLE_KIOSK_MODE:
            case GlobalSettings.KEY_KIOSK_MODE:
                updateKioskModeSummary();
                break;
            case GlobalSettings.KEY_AUDIOBOOKS_FOLDER:
                updateAudiobooksFolderSummary();
        }
    }

    private void updateVersionSummary() {
        Preference preference = getPreference(KEY_VERSION);
        String versionString = String.format(
                Locale.US, "%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        preference.setSummary(versionString);
    }

    private void updateKioskModeSummary() {
        Preference kioskModeScreen = getPreference(GlobalSettings.KEY_KIOSK_MODE_SCREEN);

        int summaryStringId = R.string.pref_kiosk_mode_screen_summary_disabled;
        if (globalSettings.isFullKioskModeEnabled())
            summaryStringId = R.string.pref_kiosk_mode_screen_summary_full;
        else if (globalSettings.isSimpleKioskModeEnabled())
            summaryStringId = R.string.pref_kiosk_mode_screen_summary_simple;
        kioskModeScreen.setSummary(summaryStringId);
    }

    private void setupAudiobooksFolder() {
        Preference preference = getPreference(KEY_AUDIOBOOKS_FOLDER);
        preference.setOnPreferenceClickListener(ignore -> {
            String currentFolder = globalSettings.audiobooksFolder();
            Uri currentFolderUri = (currentFolder != null) ? Uri.parse(currentFolder) : null;
            openDocumentTreeContract.launch(currentFolderUri);
            return true;
        });
        updateAudiobooksFolderSummary();
    }

    private void updateAudiobooksFolderSummary() {
        Preference preference = getPreference(KEY_AUDIOBOOKS_FOLDER);
        preference.setVisible(!globalSettings.legacyFileAccessMode());
        String folderUriString = globalSettings.audiobooksFolder();
        if (folderUriString != null) {
            DocumentFile documentFile = DocumentFile.fromTreeUri(requireContext(), Uri.parse(folderUriString));
            preference.setSummary(documentFile.getName());
        } else {
            preference.setSummary("");
        }
    }

    private void onAudiobooksFolderSet(@Nullable Uri documentTreeUri) {
        if (documentTreeUri != null) {
            globalSettings.setAudiobooksFolder(documentTreeUri.toString());
        }
    }

    private void setupFaq() {
        Preference preference = getPreference(KEY_FAQ);
        preference.setSummary(getString(R.string.pref_help_faq_summary, FAQ_URL));
        preference.setOnPreferenceClickListener(preference1 -> {
            openUrl(FAQ_URL);
            return true;
        });
    }
}
