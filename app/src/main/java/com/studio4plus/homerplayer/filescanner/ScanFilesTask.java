package com.studio4plus.homerplayer.filescanner;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.studio4plus.homerplayer.util.DirectoryFilter;
import com.studio4plus.homerplayer.util.FilesystemUtil;
import com.studio4plus.homerplayer.util.OrFilter;

import java.io.File;
import java.io.FileFilter;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

public class ScanFilesTask implements Callable<List<FileSet>> {

    private static final String[] SUPPORTED_SUFFIXES = {".mp3", ".m4a", ".ogg"};

    private final @NonNull Context applicationContext;
    private final @NonNull String audioBooksDirectoryName;

    ScanFilesTask(@NonNull Context applicationContext, @NonNull String audioBooksDirectoryName) {
        this.applicationContext = applicationContext;
        this.audioBooksDirectoryName = audioBooksDirectoryName;
    }

    @Override
    public List<FileSet> call() throws Exception {
        return scanAudioBooksDirectories();
    }

    private List<FileSet> scanAudioBooksDirectories() {
        List<FileSet> fileSets = new ArrayList<>();
        List<File> dirsToScan = FilesystemUtil.listRootDirs(applicationContext);
        File defaultStorage = Environment.getExternalStorageDirectory();
        if (!containsByValue(dirsToScan, defaultStorage))
            dirsToScan.add(defaultStorage);

        for (File rootDir : dirsToScan) {
            File audioBooksDir = new File(rootDir, audioBooksDirectoryName);
            scanAndAppendBooks(audioBooksDir, fileSets);
        }
        return fileSets;
    }


    private void scanAndAppendBooks(File audioBooksDir, List<FileSet> fileSets) {
        if (audioBooksDir.exists() && audioBooksDir.isDirectory() && audioBooksDir.canRead()) {
            File[] audioBookDirs = audioBooksDir.listFiles(new DirectoryFilter());
            if (audioBookDirs != null) {
                for (File directory : audioBookDirs) {
                    FileSet fileSet = createFileSet(directory);
                    if (fileSet != null && !fileSets.contains(fileSet))
                        fileSets.add(fileSet);
                }
            }
        }
    }

    @Nullable
    private FileSet createFileSet(File bookDirectory) {
        File[] allFiles = getAllAudioFiles(bookDirectory);
        int bookDirectoryPathLength = bookDirectory.getAbsolutePath().length();

        ByteBuffer bufferLong = ByteBuffer.allocate(Long.SIZE);
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            for (File file : allFiles) {
                String path = file.getAbsolutePath();
                String relativePath = path.substring(bookDirectoryPathLength);

                // TODO: what if the same book is in two directories?
                bufferLong.putLong(0, file.length());
                digest.update(relativePath.getBytes());
                digest.update(bufferLong);
            }
            String id = Base64.encodeToString(digest.digest(), Base64.NO_PADDING | Base64.NO_WRAP);
            if (allFiles.length > 0) {
                File sampleIndicator = new File(bookDirectory, FileScanner.SAMPLE_BOOK_FILE_NAME);
                boolean isDemoSample = sampleIndicator.exists();
                return new FileSet(id, bookDirectory, allFiles, isDemoSample);
            } else {
                return null;
            }
        } catch (NoSuchAlgorithmException e) {
            // Never happens.
            e.printStackTrace();
            throw new RuntimeException("MD5 not available");
        }
    }


    @NonNull
    private File[] getAllAudioFiles(File directory) {
        List<File> files = new ArrayList<>();
        FileFilter audioFiles = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return isAudioFile(pathname);
            }
        };

        FileFilter filesAndDirectoriesFilter = new OrFilter(audioFiles, new DirectoryFilter());
        addFilesRecursive(directory, filesAndDirectoriesFilter, files);
        return files.toArray(new File[files.size()]);
    }


    private void addFilesRecursive(File directory, FileFilter filter, List<File> allFiles) {
        File[] files = directory.listFiles(filter);
        // listFiles may return null. Skip such directories.
        if (files == null)
            return;

        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        });

        for (File file : files) {
            if (file.isDirectory()) {
                addFilesRecursive(file, filter, allFiles);
            } else {
                allFiles.add(file);
            }
        }
    }

    private <Type> boolean containsByValue(List<Type> items, Type needle) {
        for (Type item : items)
            if (item.equals(needle))
                return true;
        return false;
    }

    private static boolean isAudioFile(File file) {
        String lowerCaseFileName = file.getName().toLowerCase();
        for (String suffix : SUPPORTED_SUFFIXES)
            if (lowerCaseFileName.endsWith(suffix))
                return true;

        return false;
    }
}
