package com.studio4plus.audiobookplayer.model;

import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

public class AudioBookManager {

    public interface Listener {
        public void onCurrentBookChanged(AudioBook book);
    }

    private static final String AUDIOBOOKS_DIRECTORY = "AudioBooks";

    private static AudioBookManager instance;

    private final List<AudioBook> audioBooks = new ArrayList<>();
    private final WeakHashMap<Listener, Void> weakListeners = new WeakHashMap<>();
    private AudioBook currentBook;

    public static AudioBookManager getInstance() {
        if (instance == null)
            instance = new AudioBookManager();
        return instance;
    }

    private AudioBookManager() {
        initializeFromDisk();
        if (audioBooks.size() > 0)
            currentBook = audioBooks.get(0);
    }

    public List<AudioBook> getAudioBooks() {
        return audioBooks;
    }

    public void setCurrentBook(AudioBook book) {
        currentBook = book;
        for (Listener listener : weakListeners.keySet())
            listener.onCurrentBookChanged(book);
    }

    public AudioBook getCurrentBook() {
        return currentBook;
    }

    public int getCurrentBookIndex() {
        return audioBooks.indexOf(currentBook);
    }

    public AudioBook get(String directoryName) {
        for (AudioBook book : audioBooks)
            if (book.getDirectoryName().equals(directoryName))
                return book;
        return null;
    }

    public void addWeakListener(Listener listener) {
        weakListeners.put(listener, null);
    }

    public File getAbsolutePath(AudioBook book) {
        return new File(getAudioBooksDirectory(), book.getDirectoryName());
    }

    private void initializeFromDisk() {
        if (isExternalStorageReadable()) {
            File audioBooksDir = getAudioBooksDirectory();
            if (audioBooksDir.exists() && audioBooksDir.isDirectory() && audioBooksDir.canRead()) {
                File[] audioBookDirs = audioBooksDir.listFiles(new DirectoryFilter());
                for (File directory : audioBookDirs) {
                    AudioBook audioBook = createAudioBookForDirectory(directory);
                    if (audioBook != null)
                        audioBooks.add(audioBook);
                }
            }
        } else {
            // TODO: notify the user.
        }
    }

    private File getAudioBooksDirectory() {
        File externalStorage = Environment.getExternalStorageDirectory();
        return new File(externalStorage, AUDIOBOOKS_DIRECTORY);
    }

    private AudioBook createAudioBookForDirectory(File bookDirectory) {
        File[] allFiles = getAllAudioFiles(bookDirectory);
        String[] filePaths = new String[allFiles.length];
        int bookDirectoryPathLength = bookDirectory.getAbsolutePath().length();

        for (int i = 0; i < filePaths.length; ++i) {
            String path = allFiles[i].getAbsolutePath();
            String relativePath = path.substring(bookDirectoryPathLength);
            filePaths[i] = relativePath;
        }
        return filePaths.length > 0 ? new AudioBook(bookDirectory.getName(), filePaths) : null;
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

    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    private static boolean isAudioFile(File file) {
        String fileName = file.getName();
        // TODO: allow other formats
        return fileName.toLowerCase().endsWith(".mp3");
    }

    private static class OrFilter implements FileFilter {

        private final FileFilter[] filters;

        public OrFilter(FileFilter... filters) {
            this.filters = filters;
        }

        @Override
        public boolean accept(File pathname) {
            for (FileFilter filter : filters) {
                if (filter.accept(pathname))
                    return true;
            }
            return false;
        }
    }

    private static class DirectoryFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    }
}
