package com.studio4plus.homerplayer.logging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import timber.log.Timber;

public class UncaughtExceptionLogger implements Thread.UncaughtExceptionHandler {

    @Nullable
    private final Thread.UncaughtExceptionHandler previousHandler;

    public UncaughtExceptionLogger(@Nullable Thread.UncaughtExceptionHandler previousHandler) {
        this.previousHandler = previousHandler;
    }

    public static void install() {
        UncaughtExceptionLogger handler =
                new UncaughtExceptionLogger(Thread.getDefaultUncaughtExceptionHandler());
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        Timber.e(throwable, "Crash!\n");
        if (previousHandler != null) previousHandler.uncaughtException(thread, throwable);
    }
}
