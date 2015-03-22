package com.studio4plus.audiobookplayer.model;

import android.os.Environment;
import android.util.Base64;

import com.studio4plus.audiobookplayer.util.DirectoryFilter;
import com.studio4plus.audiobookplayer.util.OrFilter;

import java.io.File;
import java.io.FileFilter;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

// TODO: loading book data from storage should be moved to a separate class.
public class AudioBookManager {

    public interface Listener {
        public void onCurrentBookChanged(AudioBook book);
    }

    private static final String AUDIOBOOKS_DIRECTORY = "AudioBooks";

    private final List<AudioBook> audioBooks = new ArrayList<>();
    private final WeakHashMap<Listener, Void> weakListeners = new WeakHashMap<>();
    private final Storage storage;
    private AudioBook currentBook;

    public AudioBookManager(Storage storage) {
        this.storage = storage;
        addWeakListener(storage);
        initializeFromDisk();
        if (audioBooks.size() > 0) {
            assignColoursToNewBooks();

            String id = storage.getCurrentAudioBook();
            currentBook = getById(id);
            if (currentBook == null)
                currentBook = audioBooks.get(0);
        }
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

    public AudioBook getById(String id) {
        for (AudioBook book : audioBooks)
            if (book.getId().equals(id))
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

            Collections.sort(audioBooks, new Comparator<AudioBook>() {
                @Override
                public int compare(AudioBook lhs, AudioBook rhs) {
                    return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
                }
            });
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

        ByteBuffer bufferLong = ByteBuffer.allocate(Long.SIZE);
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            for (int i = 0; i < filePaths.length; ++i) {
                String path = allFiles[i].getAbsolutePath();
                String relativePath = path.substring(bookDirectoryPathLength);
                filePaths[i] = relativePath;

                // TODO: what if the same book is in two directories?
                bufferLong.putLong(0, allFiles[i].length());
                digest.update(relativePath.getBytes());
                digest.update(bufferLong);
            }
            String id = Base64.encodeToString(digest.digest(), Base64.NO_PADDING | Base64.NO_WRAP);
            if (filePaths.length > 0) {
                Position lastPosition = storage.getPositionForAudioBook(id);
                AudioBook book = new AudioBook(id, bookDirectory.getName(), filePaths, lastPosition);
                book.setPositionObserver(storage);
                return book;
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

    private void assignColoursToNewBooks() {
        final int MAX_NEIGHBOUR_DISTANCE = 2;

        int count = audioBooks.size();
        int lastIndex = count - 1;
        for (int i = 0; i < count; ++i) {
            AudioBook currentBook = audioBooks.get(i);
            if (currentBook.getColourScheme() == null) {
                int startNeighbourIndex = i - MAX_NEIGHBOUR_DISTANCE;
                int endNeighbourIndex = i + MAX_NEIGHBOUR_DISTANCE;
                List<ColourScheme> coloursToAvoid = getColoursInRange(
                        Math.max(0, startNeighbourIndex),
                        Math.min(lastIndex, endNeighbourIndex));
                currentBook.setColourScheme(ColourScheme.getRandom(coloursToAvoid));
            }
        }
        // TODO: save the colour info to persistent storage.
    }

    private List<ColourScheme> getColoursInRange(int startIndex, int endIndex) {
        List<ColourScheme> colours = new ArrayList<>();
        for (int i = startIndex; i <= endIndex; ++i) {
            ColourScheme colourScheme = audioBooks.get(i).getColourScheme();
            if (colourScheme != null)
                colours.add(colourScheme);
        }
        return colours;
    }
}
