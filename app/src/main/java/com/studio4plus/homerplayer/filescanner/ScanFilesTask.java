package com.studio4plus.homerplayer.filescanner;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Base64;

import com.studio4plus.homerplayer.util.DirectoryFilter;
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

    interface FolderProvider {
        @NonNull
        List<File> getFolders();
    }

    private final FolderProvider folderProvider;
    private final boolean demoSamples;

    ScanFilesTask(@NonNull FolderProvider folderProvider, boolean demoSamples) {
        this.folderProvider = folderProvider;
        this.demoSamples = demoSamples;
    }

    @Override
    public List<FileSet> call() throws Exception {
        return scanAudioBooksDirectories();
    }

    private List<FileSet> scanAudioBooksDirectories() {
        List<FileSet> fileSets = new ArrayList<>();
        for (File folder : folderProvider.getFolders()) {
            scanAndAppendBooks(folder, fileSets);
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
                Uri[] uris = new Uri[allFiles.length];
                for (int i = 0; i < allFiles.length; ++i) {
                    uris[i] = Uri.fromFile(allFiles[i]);
                }
                return new FileSet(id, bookDirectory.getName(), uris, demoSamples);
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

    private static boolean isAudioFile(File file) {
        String lowerCaseFileName = file.getName().toLowerCase();
        for (String suffix : FileScanner.SUPPORTED_SUFFIXES)
            if (lowerCaseFileName.endsWith(suffix))
                return true;

        return false;
    }
}
