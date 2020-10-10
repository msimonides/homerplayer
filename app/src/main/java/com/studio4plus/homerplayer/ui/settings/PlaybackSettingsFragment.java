package com.studio4plus.homerplayer.ui.settings;

import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.ui.SnippetPlayer;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class PlaybackSettingsFragment extends BaseSettingsFragment {

    @Nullable
    private SnippetPlayer snippetPlayer = null;

    @Inject public GlobalSettings globalSettings;
    @Inject public AudioBookManager audioBookManager;
    @Inject public EventBus eventBus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        HomerPlayerApplication.getComponent(requireActivity()).inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_playback, rootKey);

        getPreference(GlobalSettings.KEY_JUMP_BACK).setSummaryProvider(
                new FormatStringListSummaryProvider(
                        getResources(),
                        R.string.pref_jump_back_entry_disabled,
                        R.string.pref_jump_back_summary));
        getPreference(GlobalSettings.KEY_SLEEP_TIMER).setSummaryProvider(
                new FormatStringListSummaryProvider(
                        getResources(),
                        R.string.pref_sleep_timer_summary_disabled,
                        R.string.pref_sleep_timer_summary));
        getPreference(GlobalSettings.KEY_PLAYBACK_SPEED).setOnPreferenceClickListener(
                preference -> {
                    if (snippetPlayer != null && !snippetPlayer.isPlaying())
                        playSnippet();
                    return false;
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (snippetPlayer != null) {
            snippetPlayer.stop();
            snippetPlayer = null;
        }
    }

    private void playSnippet() {
        if (snippetPlayer != null) {
            snippetPlayer.stop();
            snippetPlayer = null;
        }

        AudioBook book = audioBookManager.getCurrentBook();
        if (book != null) {
            snippetPlayer = new SnippetPlayer(getActivity(), eventBus, globalSettings.getPlaybackSpeed());

            snippetPlayer.play(book);
        }
    }

    @Override
    protected int getTitle() {
        return R.string.pref_playback_options_screen_title;
    }

    private static class FormatStringListSummaryProvider
            implements Preference.SummaryProvider<ListPreference> {

        @NonNull
        private final Resources resources;
        @StringRes
        private final int stringDisabled;
        @StringRes
        private final int stringValue;

        private FormatStringListSummaryProvider(@NonNull Resources resources, int stringDisabled, int stringValue) {
            this.resources = resources;
            this.stringDisabled = stringDisabled;
            this.stringValue = stringValue;
        }

        @Override
        public CharSequence provideSummary(ListPreference preference) {
            int index = preference.findIndexOfValue(preference.getValue());
            if (index == 0) {
                return resources.getString(stringDisabled);
            } else {
                CharSequence entry = preference.getEntries()[index];
                return resources.getString(stringValue, entry);
            }
        }
    }
}
