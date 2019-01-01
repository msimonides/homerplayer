package com.studio4plus.homerplayer.ui.settings;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.WindowManager;

import com.crashlytics.android.Crashlytics;
import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.events.SettingsEnteredEvent;
import com.studio4plus.homerplayer.ui.ActivityComponent;
import com.studio4plus.homerplayer.ui.ActivityModule;
import com.studio4plus.homerplayer.ui.DaggerActivityComponent;
import com.studio4plus.homerplayer.ui.KioskModeHandler;
import com.studio4plus.homerplayer.ui.OrientationActivityDelegate;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class SettingsActivity
        extends AppCompatActivity
        implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {

    private static final int BLOCK_TIME_MS = 500;

    private Handler mainThreadHandler;
    private Runnable unblockEventsTask;
    private OrientationActivityDelegate orientationDelegate;

    @Inject public EventBus eventBus;
    @Inject public GlobalSettings globalSettings;
    @Inject public KioskModeHandler kioskModeHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.settings_activity);
        super.onCreate(savedInstanceState);
        ActivityComponent activityComponent = DaggerActivityComponent.builder()
                .applicationComponent(HomerPlayerApplication.getComponent(this))
                .activityModule(new ActivityModule(this))
                .build();
        activityComponent.inject(this);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        kioskModeHandler.setKeepNavigation(true);
        orientationDelegate = new OrientationActivityDelegate(this, globalSettings);

        // Display the fragment as the main content.
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, new MainSettingsFragment())
                .commit();

        mainThreadHandler = new Handler(getMainLooper());
    }

    @Override
    protected void onStart() {
        super.onStart();
        orientationDelegate.onStart();
        blockEventsOnStart();
        eventBus.post(new SettingsEnteredEvent());
        kioskModeHandler.onActivityStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        orientationDelegate.onStop();
        cancelBlockEventOnStart();
        kioskModeHandler.onActivityStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            kioskModeHandler.onFocusGained();
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment;
        try {
            // TODO: use FragmentFactory when updating to androidx.
            Class<Fragment> fragmentClass = (Class<Fragment>) Class.forName(pref.getFragment(), true, getClassLoader());
            Preconditions.checkState(
                    BaseSettingsFragment.class.isAssignableFrom(fragmentClass));
            fragment = fragmentClass.newInstance();

            fragment.setArguments(args);
            fragment.setTargetFragment(caller, 0);
            // Replace the existing Fragment with the new Fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_container, fragment)
                    .addToBackStack(null)
                    .commit();
            return true;
        } catch (Exception e) {
            Crashlytics.logException(e);
            return false;
        }
    }

    @Override
    public boolean onPreferenceDisplayDialog(
            @NonNull PreferenceFragmentCompat preferenceFragmentCompat, Preference preference) {
        if (preference instanceof ConfirmDialogPreference) {
            DialogFragment dialogFragment =
                    ConfirmDialogFragmentCompat.newInstance(preference.getKey());
            dialogFragment.setTargetFragment(preferenceFragmentCompat, 0);
            dialogFragment.show(getSupportFragmentManager(), "CONFIRM_DIALOG");
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
