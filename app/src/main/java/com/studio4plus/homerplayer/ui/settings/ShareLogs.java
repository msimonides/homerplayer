package com.studio4plus.homerplayer.ui.settings;

import android.content.Context;

import androidx.annotation.NonNull;

import com.studio4plus.homerplayer.ApplicationScope;
import com.studio4plus.homerplayer.concurrency.BackgroundExecutor;
import com.studio4plus.homerplayer.concurrency.SimpleFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

public class ShareLogs {

    @NonNull
    private final BackgroundExecutor ioExecutor;
    @NonNull
    private final Context appContext;

    @Inject
    public ShareLogs(
            @ApplicationScope @NonNull Context appContext,
            @Named("IO_EXECUTOR") @NonNull BackgroundExecutor ioExecutor) {
        this.ioExecutor = ioExecutor;
        this.appContext = appContext;
    }

    public SimpleFuture<File> shareLogs() {
        File logsFolder = logsFolder();
        SimpleFuture<File> resultFuture = ioExecutor.postTask(new SaveLog(logsFolder));
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

        private SaveLog(@NonNull File outputFolder) {
            this.outputFolder = outputFolder;
        }

        @Override
        public File call() throws Exception {
            if (!outputFolder.exists() && !outputFolder.mkdirs())
                throw new IOException("Unable to create temporary directory.");

            File outputFile = File.createTempFile("Homer_log_", ".txt", outputFolder);
            OutputStream outputStream = new FileOutputStream(outputFile);
            Process process = Runtime.getRuntime().exec(new String[]{ "logcat", "-b", "main", "-d", "*:I" });
            InputStream inputStream = process.getInputStream();
            try {
                byte[] buffer = new byte[65535];
                int read = inputStream.read(buffer);
                while (read != -1) {
                    outputStream.write(buffer, 0, read);
                    read = inputStream.read(buffer);
                }
            } finally {
                inputStream.close();
                outputStream.close();
            }

            return outputFile;
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
