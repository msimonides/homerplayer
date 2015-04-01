package com.studio4plus.homerplayer.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.studio4plus.homerplayer.HomerPlayerDeviceAdmin;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ApplicationLocker {

    // Use this preference to implement the missing isLockTaskEnabled.
    private static final String SCREEN_LOCK_PREFS = "ScreenLocker";
    private static final String PREF_SCREEN_LOCK_ENABLED = "screen_lock_enabled";

    public static void onActivityCreated(Activity activity) {
        if (isTaskLocked(activity))
            lockScreen(activity);
    }

    public static boolean lockApplication(Activity activity) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm.isLockTaskPermitted(activity.getPackageName())
                && dpm.isDeviceOwnerApp(activity.getPackageName())) {
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
            intentFilter.addCategory(Intent.CATEGORY_HOME);
            intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
            ComponentName adminComponentName =
                    new ComponentName(activity, HomerPlayerDeviceAdmin.class);
            ComponentName activityComponentName =
                    new ComponentName(activity, MainActivity.class);
            dpm.addPersistentPreferredActivity(
                    adminComponentName, intentFilter, activityComponentName);
            lockScreen(activity);
            return true;
        }
        return false;
    }

    public static void unlockApplication(Activity activity) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm.isLockTaskPermitted(activity.getPackageName())
                && dpm.isDeviceOwnerApp(activity.getPackageName())) {
            ComponentName adminComponentName =
                    new ComponentName(activity, HomerPlayerDeviceAdmin.class);
            dpm.clearPackagePersistentPreferredActivities(
                    adminComponentName, activity.getPackageName());
            unlockScreen(activity);
        }
    }

    public static boolean isTaskLocked(Context context) {
        return context.getSharedPreferences(SCREEN_LOCK_PREFS, Context.MODE_PRIVATE).getBoolean(
                PREF_SCREEN_LOCK_ENABLED, false);
    }

    private static void lockScreen(Activity activity) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        activity.startLockTask();
        setTaskLocked(activity, true);
    }

    private static void unlockScreen(Activity activity) {
        if (isTaskLocked(activity)) {
            activity.stopLockTask();
            setTaskLocked(activity, false);
        }
    }

    @SuppressLint("CommitPrefEdits")
    private static void setTaskLocked(Context context, boolean isLocked) {
        context.getSharedPreferences(SCREEN_LOCK_PREFS, Context.MODE_PRIVATE).edit().putBoolean(
                PREF_SCREEN_LOCK_ENABLED, isLocked).commit();
    }
}
