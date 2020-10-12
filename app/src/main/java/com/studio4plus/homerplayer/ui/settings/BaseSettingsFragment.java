package com.studio4plus.homerplayer.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
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

        final ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        Preconditions.checkNotNull(actionBar);
        actionBar.setTitle(getTitle());

    }

    @Override
    public void onStop() {
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
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
        ListPreference preference = getPreference(key);
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

    @NonNull
    protected <T extends Preference> T getPreference(@NonNull CharSequence key) {
        T preference = findPreference(key);
        Preconditions.checkNotNull(preference);
        return preference;
    }
}
