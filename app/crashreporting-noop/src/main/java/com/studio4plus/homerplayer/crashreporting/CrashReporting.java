package com.studio4plus.homerplayer.crashreporting;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

public class CrashReporting {

    public static void init(@NonNull Context context) {}
    public static void log(@NonNull String message) {}
    public static void log(int priority, @NonNull String tag, @NonNull String msg) {
        Log.println(priority, tag, msg);
    }
    public static void logException(@NonNull Throwable e) {}
}
