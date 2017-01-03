package com.studio4plus.homerplayer.model;

import android.content.Context;
import android.os.Environment;
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

import javax.inject.Inject;
import javax.inject.Named;

public class FileScanner {

    static final String SAMPLE_BOOK_FILE_NAME = ".sample";

    private final String audioBooksDirectoryPath;
    private final Context context;

    @Inject
    public FileScanner(
            @Named("AUDIOBOOKS_DIRECTORY") String audioBooksDirectoryPath,
            Context context) {
        this.audioBooksDirectoryPath = audioBooksDirectoryPath;
        this.context = context;
    }

    public List<FileSet> scanAudioBooksDirectories() {
        List<FileSet> fileSets = new ArrayList<>();
        List<File> dirsToScan = FilesystemUtil.listRootDirs(context);
        File defaultStorage = Environment.getExternalStorageDirectory();
        if (!containsByValue(dirsToScan, defaultStorage))
            dirsToScan.add(defaultStorage);

        for (File rootDir : dirsToScan) {
            File audioBooksDir = new File(rootDir, audioBooksDirectoryPath);
            scanAndAppendBooks(audioBooksDir, fileSets);
        }
        return fileSets;
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

    private void scanAndAppendBooks(File audioBooksDir, List<FileSet> fileSets) {
        if (audioBooksDir.exists() && audioBooksDir.isDirectory() && audioBooksDir.canRead()) {
            File[] audioBookDirs = audioBooksDir.listFiles(new DirectoryFilter());
            for (File directory : audioBookDirs) {
                FileSet fileSet = createFileSet(directory);
                if (fileSet != null && !fileSets.contains(fileSet))
                    fileSets.add(fileSet);
            }
        }
    }

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
                File sampleIndicator = new File(bookDirectory, SAMPLE_BOOK_FILE_NAME);
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
        String fileName = file.getName();
        // TODO: allow other formats
        return fileName.toLowerCase().endsWith(".mp3");
    }
}
