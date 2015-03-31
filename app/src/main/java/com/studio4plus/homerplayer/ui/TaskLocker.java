package com.studio4plus.homerplayer.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class TaskLocker {

    // Use this preference to implement the missing isLockTaskEnabled.
    private static final String SCREEN_LOCK_PREFS = "ScreenLocker";
    private static final String PREF_SCREEN_LOCK_ENABLED = "screen_lock_enabled";

    public static void onActivityCreated(Activity activity) {
        if (isTaskLocked(activity))
            lockScreen(activity);
    }

    public static boolean lockScreen(Activity activity) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm.isLockTaskPermitted(activity.getPackageName())) {
            activity.startLockTask();
            setTaskLocked(activity, true);
            return true;
        }
        return false;
    }

    public static void unlockScreen(Activity activity) {
        if (isTaskLocked(activity)) {
            activity.stopLockTask();
            setTaskLocked(activity, false);
        }
    }

    public static boolean isTaskLocked(Context context) {
        return context.getSharedPreferences(SCREEN_LOCK_PREFS, Context.MODE_PRIVATE).getBoolean(
                PREF_SCREEN_LOCK_ENABLED, false);
    }

    @SuppressLint("CommitPrefEdits")
    private static void setTaskLocked(Context context, boolean isLocked) {
        context.getSharedPreferences(SCREEN_LOCK_PREFS, Context.MODE_PRIVATE).edit().putBoolean(
                PREF_SCREEN_LOCK_ENABLED, isLocked).commit();
    }
}
