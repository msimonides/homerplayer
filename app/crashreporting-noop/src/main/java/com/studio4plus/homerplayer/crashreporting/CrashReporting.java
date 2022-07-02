package com.studio4plus.homerplayer.crashreporting;

import android.content.Context;

import androidx.annotation.NonNull;

import timber.log.Timber;

public class CrashReporting {

    public static void init(@NonNull Context context) {}
    public static void log(@NonNull String message) {}
    public static void log(int priority, @NonNull String tag, @NonNull String msg) {}
    public static void logException(@NonNull Throwable e) {
        Timber.e(e);
    }
}
