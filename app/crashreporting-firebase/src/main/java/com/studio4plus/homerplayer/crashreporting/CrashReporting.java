package com.studio4plus.homerplayer.crashreporting;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

// TODO: consider changing the static functions API to instance methods.
public class CrashReporting {

    private static FirebaseCrashlytics crashlytics;

    public static void init(@NonNull Context context) {
        FirebaseApp.initializeApp(context);
        crashlytics = FirebaseCrashlytics.getInstance();
    }

    public static void log(@NonNull String message) {
        crashlytics.log(message);
    }

    @Deprecated
    public static void log(int priority, @NonNull String tag, @NonNull String msg) {
        crashlytics.log(tag + ": " + msg);
        Log.println(priority, tag, msg);
    }

    public static void logException(@NonNull Throwable e) {
        crashlytics.recordException(e);
    }
}
