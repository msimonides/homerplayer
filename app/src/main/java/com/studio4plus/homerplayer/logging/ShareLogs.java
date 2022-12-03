package com.studio4plus.homerplayer.logging;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import com.michaelflisar.lumberjack.FileLoggingSetup;
import com.studio4plus.homerplayer.ApplicationScope;
import com.studio4plus.homerplayer.BuildConfig;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.concurrency.BackgroundExecutor;
import com.studio4plus.homerplayer.concurrency.SimpleFuture;
import com.studio4plus.homerplayer.crashreporting.CrashReporting;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

public class ShareLogs {

    @NonNull
    private final BackgroundExecutor ioExecutor;
    @NonNull
    private final FileLoggingSetup fileLoggingSetup;
    @NonNull
    private final GlobalSettings globalSettings;
    @NonNull
    private final Context appContext;

    @Inject
    public ShareLogs(
            @ApplicationScope @NonNull Context appContext,
            @NonNull FileLoggingSetup fileLoggingSetup,
            @Named("IO_EXECUTOR") @NonNull BackgroundExecutor ioExecutor,
            @NonNull GlobalSettings globalSettings) {
        this.ioExecutor = ioExecutor;
        this.fileLoggingSetup = fileLoggingSetup;
        this.globalSettings = globalSettings;
        this.appContext = appContext;
    }

    public SimpleFuture<File> shareLogs() {
        File logsFolder = logsFolder();
        SimpleFuture<File> resultFuture = ioExecutor.postTask(new SaveLog(logsFolder, fileLoggingSetup, globalSettings));
        ioExecutor.postTask(new ClearOldLogs(logsFolder));
        return resultFuture;
    }

    @NonNull
    private File logsFolder() {
        return new File(appContext.getFilesDir(), "shared");
    }

    private static class SaveLog implements Callable<File> {

        @NonNull
        private final File outputFolder;
        @NonNull
        private final FileLoggingSetup fileLoggingSetup;
        @NonNull
        private final GlobalSettings globalSettings;

        private SaveLog(@NonNull File outputFolder,
                        @NonNull FileLoggingSetup fileLoggingSetup,
                        @NonNull GlobalSettings globalSettings) {
            this.outputFolder = outputFolder;
            this.fileLoggingSetup = fileLoggingSetup;
            this.globalSettings = globalSettings;
        }

        @Override
        public File call() throws Exception {
            if (!outputFolder.exists() && !outputFolder.mkdirs())
                throw new IOException("Unable to create temporary directory.");

            File outputFile = File.createTempFile("Homer_log_", ".txt", outputFolder);
            OutputStream outputStream = new FileOutputStream(outputFile);
            List<File> files = fileLoggingSetup.getAllExistingLogFiles();
            try {
                for (File file : files) {
                    append(outputStream, file);
                }
            } finally {
                appendStatus(outputStream);
                outputStream.close();
            }
            return outputFile;
        }

        private void append(@NonNull OutputStream outputStream, @NonNull File file) {
            try {
                InputStream inputStream = new FileInputStream(file);
                try {
                    byte[] buffer = new byte[65535];
                    int read = inputStream.read(buffer);
                    while (read != -1) {
                        outputStream.write(buffer, 0, read);
                        read = inputStream.read(buffer);
                    }
                } finally {
                    inputStream.close();
                }
            } catch(IOException e) {
                PrintWriter writer = new PrintWriter(outputStream);
                writer.println("Error while appending file " + file.getName());
                writer.println("  " + e);
            }
        }

        private void appendStatus(@NonNull OutputStream outputStream) {
            PrintWriter writer = new PrintWriter(outputStream);
            writer.println("Manufacturer: " + Build.MANUFACTURER + "; " + Build.BRAND);
            writer.println("Model: " + Build.MODEL);
            writer.println("Android API: " + Build.VERSION.SDK_INT);
            writer.println("App Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ") " + BuildConfig.FLAVOR);
            writer.println("Legacy file access mode: " + globalSettings.legacyFileAccessMode());
            writer.println("Audiobooks folders: " + globalSettings.audiobooksFolders());
            String crashreporting = CrashReporting.statusForDiagnosticLog();
            if (crashreporting != null) {
                writer.println(crashreporting);
            }
            writer.flush();
        }
    }

    private static class ClearOldLogs implements Callable<Void> {

        @NonNull
        private final File folder;

        private ClearOldLogs(@NonNull File folder) {
            this.folder = folder;
        }

        @Override
        public Void call() throws Exception {
            long now = (new Date()).getTime();
            File[] files = folder.listFiles();
            if (files == null) return null;

            for(File file : files) {
                if (file.lastModified() < now - TimeUnit.DAYS.toMillis(1)) {
                    file.delete();
                }
            }
            return null;
        }
    }
}
