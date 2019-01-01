package com.studio4plus.homerplayer.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.R;

import java.util.Objects;

abstract class BaseSettingsFragment
        extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onStart() {
        super.onStart();
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        Preconditions.checkNotNull(actionBar);
        actionBar.setTitle(getTitle());

    }

    @Override
    public void onStop() {
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    @NonNull
    protected SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(getActivity()));
    }

    @StringRes
    protected abstract int getTitle();

    protected void updateListPreferenceSummary(@NonNull SharedPreferences sharedPreferences,
                                               @NonNull String key,
                                               int default_value_res_id) {
        String stringValue = sharedPreferences.getString(key, getString(default_value_res_id));
        ListPreference preference =
                (ListPreference) findPreference(key);
        int index = preference.findIndexOfValue(stringValue);
        if (index < 0)
            index = 0;
        preference.setSummary(preference.getEntries()[index]);
    }

    protected void openUrl(@NonNull String url) {
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
}
