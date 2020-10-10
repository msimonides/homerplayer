package com.studio4plus.homerplayer.ui.settings;

import android.os.Bundle;

import com.studio4plus.homerplayer.R;

public class UiSettingsFragment extends BaseSettingsFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_ui, rootKey);
    }

    @Override
    protected int getTitle() {
        return R.string.pref_ui_options_screen_title;
    }
}
