package com.studio4plus.homerplayer;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.studio4plus.homerplayer.model.LibraryContentType;
import com.studio4plus.homerplayer.ui.ColorTheme;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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

    // TODO: figure out if these constants can somehow be shared with the keys in xml files.
    public static final String KEY_COLOR_THEME = "color_theme";
    public static final String KEY_KIOSK_MODE_SCREEN = "kiosk_mode_screen";
    public static final String KEY_KIOSK_MODE = "kiosk_mode_preference";
    public static final String KEY_SIMPLE_KIOSK_MODE = "simple_kiosk_mode_preference";
    public static final String KEY_JUMP_BACK = "jump_back_preference";
    public static final String KEY_SLEEP_TIMER = "sleep_timer_preference";
    public static final String KEY_SCREEN_ORIENTATION = "screen_orientation_preference";
    public static final String KEY_FF_REWIND_SOUND = "ff_rewind_sound_preference";
    public static final String KEY_PLAYBACK_SPEED = "playback_speed_preference";
    public static final String KEY_VOLUME_CONTROLS = "volume_controls_preference";

    private static final String KEY_BROWSING_HINT_SHOWN = "hints.browsing_hint_shown";
    private static final String KEY_SETTINGS_HINT_SHOWN = "hints.settings.hint_shown";
    private static final String KEY_FLIPTOSTOP_HINT_SHOWN = "hints.fliptostop.hint_shown";

    public static final String KEY_AUDIOBOOKS_FOLDERS = "audiobooks_folders";

    private static final String KEY_BOOKS_EVER_INSTALLED = "action_history.books_ever_installed";
    private static final String KEY_SETTINGS_EVER_ENTERED = "action_history.settings_ever_entered";
    private static final String KEY_LAST_STARTED_VERSION_CODE = "last_version_code";
    public static final String KEY_LEGACY_FILE_ACCESS_MODE = "legacy_file_access";

    private final Resources resources;
    private final SharedPreferences sharedPreferences;

    @Inject
    public GlobalSettings(
            @NonNull Resources resources,
            @NonNull SharedPreferences sharedPreferences) {
        this.resources = resources;
        this.sharedPreferences = sharedPreferences;
    }

    public Set<String> audiobooksFolders() {
        return sharedPreferences.getStringSet(KEY_AUDIOBOOKS_FOLDERS, Collections.emptySet());
    }

    public void addAudiobooksFolder(@NonNull String newFolderUri) {
        setLegacyFileAccessMode(false);
        Set<String> folderUris = new HashSet<>(audiobooksFolders());
        folderUris.add(newFolderUri);
        sharedPreferences.edit().putStringSet(KEY_AUDIOBOOKS_FOLDERS, folderUris).apply();
    }

    public boolean removeAudiobooksFolder(@NonNull String folderUri) {
        Set<String> folderUris = new HashSet<>(audiobooksFolders());
        boolean isRemoved = folderUris.remove(folderUri);
        if (isRemoved)
            sharedPreferences.edit().putStringSet(KEY_AUDIOBOOKS_FOLDERS, folderUris).apply();
        return isRemoved;
    }

    public boolean legacyFileAccessMode() {
        return sharedPreferences.getBoolean(KEY_LEGACY_FILE_ACCESS_MODE, false);
    }

    public void setLegacyFileAccessMode(boolean isEnabled) {
        sharedPreferences.edit().putBoolean(KEY_LEGACY_FILE_ACCESS_MODE, isEnabled).apply();
    }

    public int getJumpBackPreferenceMs() {
        String valueString = sharedPreferences.getString(
                KEY_JUMP_BACK, resources.getString(R.string.pref_jump_back_default_value));
        return (int) TimeUnit.SECONDS.toMillis(Integer.parseInt(valueString));
    }

    public long getSleepTimerMs() {
        String valueString = sharedPreferences.getString(
                KEY_SLEEP_TIMER, resources.getString(R.string.pref_sleep_timer_default_value));
        return (long) TimeUnit.SECONDS.toMillis(Long.parseLong(valueString));
    }

    public int getScreenOrientation() {
        String stringValue = sharedPreferences.getString(
                GlobalSettings.KEY_SCREEN_ORIENTATION,
                resources.getString(R.string.pref_screen_orientation_default_value));
        return Orientation.valueOf(stringValue).value;
    }

    public float getPlaybackSpeed() {
        final String valueString = sharedPreferences.getString(
                KEY_PLAYBACK_SPEED, resources.getString(R.string.pref_playback_speed_default_value));
        return Float.parseFloat(valueString);
    }

    public LibraryContentType booksEverInstalled() {
        try {
            String value = sharedPreferences.getString(KEY_BOOKS_EVER_INSTALLED, null);
            if (value != null)
                return LibraryContentType.valueOf(value);
            else
                return LibraryContentType.EMPTY;
        } catch (ClassCastException e) {
            boolean everInstalled = sharedPreferences.getBoolean(KEY_BOOKS_EVER_INSTALLED, false);
            LibraryContentType contentType =
                    everInstalled ? LibraryContentType.USER_CONTENT : LibraryContentType.EMPTY;
            setBooksEverInstalled(contentType);
            return contentType;
        }
    }

    public void setBooksEverInstalled(LibraryContentType contentType) {
        LibraryContentType oldContentType = booksEverInstalled();
        if (contentType.supersedes(oldContentType))
            sharedPreferences.edit().putString(KEY_BOOKS_EVER_INSTALLED, contentType.name()).apply();
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

    public boolean isFullKioskModeEnabled() {
        return sharedPreferences.getBoolean(KEY_KIOSK_MODE, false);
    }

    @SuppressLint("ApplySharedPref")
    public void setFullKioskModeEnabledNow(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_KIOSK_MODE, enabled).commit();
    }

    public boolean isSimpleKioskModeEnabled() {
        return sharedPreferences.getBoolean(KEY_SIMPLE_KIOSK_MODE, false);
    }

    public boolean isAnyKioskModeEnabled() {
        return isSimpleKioskModeEnabled() || sharedPreferences.getBoolean(KEY_KIOSK_MODE, false);
    }

    public boolean isFFRewindSoundEnabled() {
        return sharedPreferences.getBoolean(KEY_FF_REWIND_SOUND, true);
    }

    public boolean isVolumeControlEnabled() {
        return sharedPreferences.getBoolean(KEY_VOLUME_CONTROLS, true);
    }

    public void setVolumeControlsEnabled(boolean isEnabled) {
        sharedPreferences.edit().putBoolean(KEY_VOLUME_CONTROLS, isEnabled).apply();
    }

    public long lastVersionCode() {
        return sharedPreferences.getLong(KEY_LAST_STARTED_VERSION_CODE, 0L);
    }

    public void setLastVersionCode(long versionCode) {
        sharedPreferences.edit().putLong(KEY_LAST_STARTED_VERSION_CODE, versionCode).apply();
    }

    @NonNull
    public ColorTheme colorTheme() {
        String colorThemeName = sharedPreferences.getString(
                KEY_COLOR_THEME, resources.getString(R.string.pref_color_theme_default_value));
        try {
            return ColorTheme.valueOf(colorThemeName);
        } catch (IllegalArgumentException illegalArgument) {
            return ColorTheme.valueOf(resources.getString(R.string.pref_color_theme_default_value));
        }
    }
}
