package com.studio4plus.homerplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.studio4plus.homerplayer.ui.SettingsActivity;

public class GlobalSettings {

    // TODO: figure out if these constants can somehow be shared with the keys in preferences.xml
    public static final String KEY_KIOSK_MODE = "kiosk_mode_preference";
    public static final String KEY_JUMP_BACK = "jump_back_preference";

    public static int getJumpBackPreferenceMs(Context context) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        String valueString = sharedPreferences.getString(
                KEY_JUMP_BACK, context.getString(R.string.pref_jump_back_default_value));
        int valueS = Integer.parseInt(valueString);
        return valueS * 1000;
    }
}
