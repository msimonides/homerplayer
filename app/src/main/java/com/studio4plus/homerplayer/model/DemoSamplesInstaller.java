package com.studio4plus.homerplayer.model;

import android.content.Context;
import android.media.MediaScannerConnection;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.google.common.io.Files;
import com.studio4plus.homerplayer.crashreporting.CrashReporting;
import com.studio4plus.homerplayer.demosamples.DemoSamplesFolderProvider;
import com.studio4plus.homerplayer.demosamples.Unzipper;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Locale;

import javax.inject.Inject;

public class DemoSamplesInstaller {

    private final Locale locale;
    private final Context context;
    private final DemoSamplesFolderProvider samplesFolderProvider;

    @Inject
    @MainThread
    public DemoSamplesInstaller(
            @NonNull Context context,
            @NonNull Locale locale,
            @NonNull DemoSamplesFolderProvider samplesFolderProvider) {
        this.context = context;
        this.locale = locale;
        this.samplesFolderProvider = samplesFolderProvider;
    }

    @WorkerThread
    public void installBooksFromZip(File zipPath) throws IOException {
        File tempFolder = ensureTempFolder();
        InputStream inputStream = new BufferedInputStream(new FileInputStream(zipPath));
        File samplesFolder = samplesFolderProvider.demoFolder();
        boolean success =
                Unzipper.unzip(inputStream, tempFolder.getAbsoluteFile())
                && installBooks(samplesFolder, tempFolder);
        deleteFolderRecursively(tempFolder);
        if (!success) {
            deleteFolderRecursively(samplesFolder);
        }
    }

    @WorkerThread
    private boolean installBooks(@NonNull File destinationDirectory, @NonNull File sourceDirectory) {
        boolean anythingInstalled = false;
        File[] books = sourceDirectory.listFiles();
        for (File bookDirectory : books) {
            boolean success = installSingleBook(bookDirectory, destinationDirectory);
            if (success)
                anythingInstalled = true;
        }

        return anythingInstalled;
    }

    @WorkerThread
    private boolean installSingleBook(File sourceBookDirectory, File audioBooksDirectory) {
        File titlesFile = new File(sourceBookDirectory, TITLES_FILE_NAME);
        String localizedTitle = readLocalizedTitle(titlesFile, locale);

        if (localizedTitle == null)
            return false; // Malformed package.

        File bookDirectory = new File(audioBooksDirectory, localizedTitle);

        if (bookDirectory.exists())
            return false;

        if (!bookDirectory.mkdirs())
            return false;

        try {
            File files[] = sourceBookDirectory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return !TITLES_FILE_NAME.equals(filename);
                }
            });
            int count = files.length;
            String installedFilePaths[] = new String[count];

            for (int i = 0; i < count; ++i) {
                File srcFile = files[i];
                File dstFile = new File(bookDirectory, srcFile.getName());
                Files.copy(srcFile, dstFile);
                installedFilePaths[i] = dstFile.getAbsolutePath();
            }

            MediaScannerConnection.scanFile(context, installedFilePaths, null, null);

            return true;
        } catch(IOException exception) {
            deleteFolderRecursively(bookDirectory);
            CrashReporting.logException(exception);
            return false;
        }
    }

    @WorkerThread
    private String readLocalizedTitle(File file, Locale locale) {
        try {
            String titlesString = Files.toString(file, Charset.forName(TITLES_FILE_CHARSET));
            JSONObject titles = (JSONObject) new JSONTokener(titlesString).nextValue();
            String languageCode = locale.getLanguage();
            String localizedTitle = titles.optString(languageCode, null);
            if (localizedTitle == null)
                localizedTitle = titles.getString(DEFAULT_TITLE_FIELD);

            return localizedTitle;
        } catch(IOException | JSONException | ClassCastException exception) {
            CrashReporting.logException(exception);
            return null;
        }
    }

    /**
     * Deletes files from a directory, non-recursive.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteFolderRecursively(File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteFolderRecursively(file);
            } else {
                file.delete();
            }
        }
        directory.delete();
    }

    private File ensureTempFolder() {
        File cacheDir = context.getCacheDir();
        File tempFolder = new File(cacheDir, "demo_samples_tmp");
        if (!tempFolder.exists()) {
            tempFolder.mkdirs();
        }
        return tempFolder;
    }

    private static final String TITLES_FILE_NAME = "titles.json";
    private static final String TITLES_FILE_CHARSET = "UTF-8";
    private static final String DEFAULT_TITLE_FIELD = "default";
}
