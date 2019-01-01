package com.studio4plus.homerplayer.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;

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
        HomerPlayerApplication.getComponent(getActivity()).inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_playback, rootKey);

        findPreference(GlobalSettings.KEY_PLAYBACK_SPEED).setOnPreferenceClickListener(
                preference -> {
                    if (snippetPlayer != null && !snippetPlayer.isPlaying())
                        playSnippet();
                    return false;
                });

        SharedPreferences sharedPreferences = getSharedPreferences();
        updatePlaybackSpeedSummary(sharedPreferences);
        updateJumpBackSummary(sharedPreferences);
        updateSleepTimerSummary();
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // TODO: use summary updaters when updating to androix.
        switch(key) {
            case GlobalSettings.KEY_JUMP_BACK:
                updateJumpBackSummary(sharedPreferences);
                break;
            case GlobalSettings.KEY_SLEEP_TIMER:
                updateSleepTimerSummary();
                break;
            case GlobalSettings.KEY_PLAYBACK_SPEED:
                updatePlaybackSpeedSummary(sharedPreferences);
                playSnippet();
                break;
        }
    }

    private void updatePlaybackSpeedSummary(@NonNull SharedPreferences sharedPreferences) {
        updateListPreferenceSummary(
                sharedPreferences,
                GlobalSettings.KEY_PLAYBACK_SPEED,
                R.string.pref_playback_speed_default_value);
    }

    private void updateJumpBackSummary(@NonNull SharedPreferences sharedPreferences) {
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

    private void updateSleepTimerSummary() {
        ListPreference preference = (ListPreference) findPreference(GlobalSettings.KEY_SLEEP_TIMER);
        int index = preference.findIndexOfValue(preference.getValue());
        if (index == 0) {
            preference.setSummary(getString(R.string.pref_sleep_timer_summary_disabled));
        } else {
            CharSequence entry = preference.getEntries()[index];
            preference.setSummary(String.format(
                    getString(R.string.pref_sleep_timer_summary), entry));
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
}
