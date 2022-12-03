package com.studio4plus.homerplayer.logging;

import android.content.Context;

import androidx.annotation.NonNull;

import com.michaelflisar.lumberjack.FileLoggingSetup;

public class LoggingSetup {

    @NonNull
    public static FileLoggingSetup createLoggingSetup(@NonNull Context applicationContext) {
        return new FileLoggingSetup.NumberedFiles(
                applicationContext.getFileStreamPath("").getAbsolutePath(),
                true,
                "500KB",
                new FileLoggingSetup.Setup(2, "%d %marker%-5level %msg%n", "log", "log"));
    }
}
