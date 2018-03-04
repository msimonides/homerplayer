package com.studio4plus.homerplayer.filescanner;

import android.content.Context;
import android.os.Environment;

import com.studio4plus.homerplayer.ApplicationScope;
import com.studio4plus.homerplayer.concurrency.BackgroundExecutor;
import com.studio4plus.homerplayer.concurrency.SimpleFuture;

import java.io.File;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScope
public class FileScanner {
    public static final String SAMPLE_BOOK_FILE_NAME = ".sample";

    private final String audioBooksDirectoryPath;
    private final BackgroundExecutor ioExecutor;
    private final Context applicationContext;

    @Inject
    public FileScanner(
            @Named("AUDIOBOOKS_DIRECTORY") String audioBooksDirectoryPath,
            @Named("IO_EXECUTOR") BackgroundExecutor ioExecutor,
            Context applicationContext) {
        this.audioBooksDirectoryPath = audioBooksDirectoryPath;
        this.ioExecutor = ioExecutor;
        this.applicationContext = applicationContext;
    }

    public SimpleFuture<List<FileSet>> scanAudioBooksDirectories() {
        ScanFilesTask task = new ScanFilesTask(applicationContext, audioBooksDirectoryPath);
        return ioExecutor.postTask(task);
    }

    /**
     * Provide the default directory for audio books.
     *
     * The directory is in the devices external storage. Other than that there is nothing
     * special about it (e.g. it may be on an removable storage).
     */
    public File getDefaultAudioBooksDirectory() {
        File externalStorage = Environment.getExternalStorageDirectory();
        return new File(externalStorage, audioBooksDirectoryPath);
    }
}
