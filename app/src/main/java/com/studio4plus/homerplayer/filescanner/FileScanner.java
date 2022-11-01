package com.studio4plus.homerplayer.filescanner;

import static com.studio4plus.homerplayer.util.CollectionUtils.map;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.studio4plus.homerplayer.ApplicationScope;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.concurrency.BackgroundExecutor;
import com.studio4plus.homerplayer.concurrency.SimpleFuture;
import com.studio4plus.homerplayer.demosamples.DemoSamplesFolderProvider;
import com.studio4plus.homerplayer.ui.settings.AudiobooksFolderManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScope
public class FileScanner {

    public static final Collection<String> SUPPORTED_SUFFIXES = Arrays.asList(".m4a", ".m4b", ".mp3", ".ogg");

    private final GlobalSettings globalSettings;
    private final BackgroundExecutor ioExecutor;
    private final Context applicationContext;
    private final AudiobooksFolderManager folderManager;

    @Inject
    public FileScanner(
            @Named("IO_EXECUTOR") BackgroundExecutor ioExecutor,
            @NonNull GlobalSettings globalSettings,
            @NonNull Context applicationContext,
            @NonNull AudiobooksFolderManager folderManager) {
        this.ioExecutor = ioExecutor;
        this.globalSettings = globalSettings;
        this.applicationContext = applicationContext;
        this.folderManager = folderManager;
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
        List<DocumentFile> audiobooksFolders = folderManager.getCurrentFolders();
        final Callable<List<FileSet>> task;
        if (!audiobooksFolders.isEmpty()) {
            task = new ScanDocumentTreeTask(applicationContext, map(audiobooksFolders, DocumentFile::getUri));
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
