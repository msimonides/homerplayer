package com.studio4plus.homerplayer.filescanner;

import static com.studio4plus.homerplayer.util.CollectionUtils.map;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.studio4plus.homerplayer.ApplicationScope;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.concurrency.BackgroundExecutor;
import com.studio4plus.homerplayer.concurrency.SimpleFuture;
import com.studio4plus.homerplayer.demosamples.DemoSamplesFolderProvider;

import java.util.Collections;
import java.util.List;
import java.util.Set;
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

    public SimpleFuture<List<FileSet>> scanAudioBooksDirectories() {
        DemoSamplesFolderProvider samplesFolderProvider = new DemoSamplesFolderProvider(applicationContext);
        ScanFilesTask.FolderProvider demoFolderProvider =
                () -> Collections.singletonList(samplesFolderProvider.demoFolder());
        if (globalSettings.legacyFileAccessMode()) {
            final Callable<List<FileSet>> task =
                    new ScanWithFallbackTask(
                            new ScanFilesTask(new LegacyFolderProvider(applicationContext), false),
                            new ScanFilesTask(demoFolderProvider, true));
            return ioExecutor.postTask(task);
        }
        Set<String> audiobooksFolderStrings = globalSettings.audiobooksFolders();
        final Callable<List<FileSet>> task;
        if (!audiobooksFolderStrings.isEmpty()) {
            task = new ScanDocumentTreeTask(applicationContext, map(audiobooksFolderStrings, Uri::parse));
        } else {
            task = new ScanFilesTask(demoFolderProvider, true);
        }
        return ioExecutor.postTask(task);
    }

    private static class ScanWithFallbackTask implements Callable<List<FileSet>> {

        private final Callable<List<FileSet>> scanMainContentTask;
        private final Callable<List<FileSet>> scanDemoSamplesTask;

        private ScanWithFallbackTask(
                Callable<List<FileSet>> scanMainContentTask,
                Callable<List<FileSet>> scanDemoSamplesTask) {
            this.scanMainContentTask = scanMainContentTask;
            this.scanDemoSamplesTask = scanDemoSamplesTask;
        }

        @Override
        public List<FileSet> call() throws Exception {
            List<FileSet> mainContent = scanMainContentTask.call();
            if (mainContent.isEmpty()) {
                return scanDemoSamplesTask.call();
            }
            return mainContent;
        }
    }
}
