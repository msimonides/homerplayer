package com.studio4plus.homerplayer;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GlobalSettings {

    private enum Orientation {
        LANDSCAPE_AUTO(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE),
        LANDSCAPE_LOCKED(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
        LANDSCAPE_REVERSE_LOCKED(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

        public final int value;

        Orientation(int value) {
            this.value = value;
        }
    }

    // TODO: figure out if these constants can somehow be shared with the keys in preferences.xml
    public static final String KEY_KIOSK_MODE = "kiosk_mode_preference";
    public static final String KEY_JUMP_BACK = "jump_back_preference";
    public static final String KEY_SCREEN_ORIENTATION = "screen_orientation_preference";

    private static final String KEY_BROWSING_HINT_SHOWN = "hints.browsing_hint_shown";
    private static final String KEY_SETTINGS_HINT_SHOWN = "hints.settings.hint_shown";
    private static final String KEY_FLIPTOSTOP_HINT_SHOWN = "hints.fliptostop.hint_shown";

    private static final String KEY_BOOKS_EVER_INSTALLED = "action_history.books_ever_installed";
    private static final String KEY_SETTINGS_EVER_ENTERED = "action_history.settings_ever_entered";

    private final Resources resources;
    private final SharedPreferences sharedPreferences;

    @Inject
    public GlobalSettings(Resources resources, SharedPreferences sharedPreferences) {
        this.resources = resources;
        this.sharedPreferences = sharedPreferences;
    }

    public int getJumpBackPreferenceMs() {
        String valueString = sharedPreferences.getString(
                KEY_JUMP_BACK, resources.getString(R.string.pref_jump_back_default_value));
        return (int) TimeUnit.SECONDS.toMillis(Integer.parseInt(valueString));
    }

    public int getScreenOrientation() {
        String stringValue = sharedPreferences.getString(
                GlobalSettings.KEY_SCREEN_ORIENTATION,
                resources.getString(R.string.pref_screen_orientation_default_value));
        return Orientation.valueOf(stringValue).value;
    }

    public boolean booksEverInstalled() {
        return sharedPreferences.getBoolean(KEY_BOOKS_EVER_INSTALLED, false);
    }

    public void setBooksEverInstalled() {
        sharedPreferences.edit().putBoolean(KEY_BOOKS_EVER_INSTALLED, true).apply();
    }

    public boolean settingsEverEntered() {
        return sharedPreferences.getBoolean(KEY_SETTINGS_EVER_ENTERED, false);
    }

    public void setSettingsEverEntered() {
        sharedPreferences.edit().putBoolean(KEY_SETTINGS_EVER_ENTERED, true).apply();
    }

    public boolean browsingHintShown() {
        return sharedPreferences.getBoolean(KEY_BROWSING_HINT_SHOWN, false);
    }

    public void setBrowsingHintShown() {
        sharedPreferences.edit().putBoolean(KEY_BROWSING_HINT_SHOWN, true).apply();
    }

    public boolean settingsHintShown() {
        return sharedPreferences.getBoolean(KEY_SETTINGS_HINT_SHOWN, false);
    }

    public void setSettingsHintShown() {
        sharedPreferences.edit().putBoolean(KEY_SETTINGS_HINT_SHOWN, true).apply();
    }

    public boolean flipToStopHintShown() {
        return sharedPreferences.getBoolean(KEY_FLIPTOSTOP_HINT_SHOWN, false);
    }

    public void setFlipToStopHintShown() {
        sharedPreferences.edit().putBoolean(KEY_FLIPTOSTOP_HINT_SHOWN, true).apply();
    }
}
