package com.studio4plus.homerplayer.crashreporting;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

import io.sentry.Sentry;
import io.sentry.SentryLevel;
import io.sentry.android.core.SentryAndroid;
import io.sentry.android.timber.SentryTimberIntegration;
import io.sentry.protocol.User;

public class CrashReporting {

    private static final String INSTALLATION_ID_KEY = "installation_id";

    private static String installationId;

    public static void init(@NonNull Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences("sentry_crashreporting", Context.MODE_PRIVATE);
        if (prefs.contains(INSTALLATION_ID_KEY)) {
            installationId = prefs.getString(INSTALLATION_ID_KEY, "");
        } else {
            installationId = UUID.randomUUID().toString();
            prefs.edit().putString(INSTALLATION_ID_KEY, installationId).apply();
        }
        SentryAndroid.init(context, options -> {
            options.setDsn(context.getString(R.string.sentry_dsn));
            options.addIntegration(
                    new SentryTimberIntegration(SentryLevel.ERROR, SentryLevel.INFO)
            );
        });
        User user = new User();
        user.setId(installationId);
        Sentry.setUser(user);
        Sentry.setTag("device.brand", Build.BRAND);
    }
    public static void log(@NonNull String message) {}
    public static void log(int priority, @NonNull String tag, @NonNull String msg) {}
    public static void logException(@NonNull Throwable e) {
        Sentry.captureException(e);
    }

    @Nullable
    public static String statusForDiagnosticLog() {
        return installationId != null ? "Sentry ID: " + installationId : null;
    }
}
