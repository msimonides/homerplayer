package com.studio4plus.homerplayer.filescanner;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.studio4plus.homerplayer.ApplicationScope;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.concurrency.BackgroundExecutor;
import com.studio4plus.homerplayer.concurrency.SimpleFuture;
import com.studio4plus.homerplayer.demosamples.DemoSamplesFolderProvider;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScope
public class FileScanner {
    private final GlobalSettings globalSettings;
    private final BackgroundExecutor ioExecutor;
    private final Context applicationContext;

    @Inject
    public FileScanner(
            @Named("IO_EXECUTOR") BackgroundExecutor ioExecutor,
            @NonNull GlobalSettings globalSettings,
            @NonNull Context applicationContext) {
        this.ioExecutor = ioExecutor;
        this.globalSettings = globalSettings;
        this.applicationContext = applicationContext;
    }

    // TODO: implement legacy scanning for apps that were updated from an earlier version.
    public SimpleFuture<List<FileSet>> scanAudioBooksDirectories() {
        String audiobooksFolderString = globalSettings.audiobooksFolder();
        if (audiobooksFolderString != null) {
            final Callable<List<FileSet>> task =
                    new ScanDocumentTreeTask(applicationContext, Uri.parse(audiobooksFolderString));
            return ioExecutor.postTask(task);
        } else {
            DemoSamplesFolderProvider samplesFolderProvider = new DemoSamplesFolderProvider(applicationContext);
            final Callable<List<FileSet>> task = new ScanFilesTask(
                    () -> Collections.singletonList(samplesFolderProvider.demoFolder()),
                    true);
            return ioExecutor.postTask(task);
        }
    }
}
