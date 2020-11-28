package com.studio4plus.homerplayer.model;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.studio4plus.homerplayer.ApplicationScope;
import com.studio4plus.homerplayer.concurrency.SimpleFuture;
import com.studio4plus.homerplayer.crashreporting.CrashReporting;
import com.studio4plus.homerplayer.events.AudioBooksChangedEvent;
import com.studio4plus.homerplayer.events.CurrentBookChangedEvent;
import com.studio4plus.homerplayer.events.MediaStoreUpdateEvent;
import com.studio4plus.homerplayer.filescanner.FileScanner;
import com.studio4plus.homerplayer.filescanner.FileSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

@ApplicationScope
public class AudioBookManager {

    @NonNull
    private final List<AudioBook> audioBooks = new ArrayList<>();
    @NonNull
    private final FileScanner fileScanner;
    @NonNull
    private final Storage storage;
    @Nullable
    private AudioBook currentBook;
    private boolean isInitialized = false;
    private boolean isFirstScan = true;
    @NonNull
    private final EventBus eventBus;

    @Inject
    @MainThread
    public AudioBookManager(
            @NonNull EventBus eventBus, @NonNull FileScanner fileScanner, @NonNull Storage storage) {
        this.fileScanner = fileScanner;
        this.storage = storage;
        this.eventBus = eventBus;
        eventBus.register(this);
    }

    @SuppressWarnings("UnusedDeclaration")
    @MainThread
    public void onEvent(MediaStoreUpdateEvent ignored) {
        scanFiles();
    }

    @MainThread
    @NonNull
    public List<AudioBook> getAudioBooks() {
        return audioBooks;
    }

    @MainThread
    public void setCurrentBook(@NonNull String bookId) {
        AudioBook newBook = getById(bookId);
        if (newBook != currentBook) {
            currentBook = getById(bookId);
            eventBus.post(new CurrentBookChangedEvent(currentBook));
        }
    }

    @MainThread
    @Nullable
    public AudioBook getCurrentBook() {
        return currentBook;
    }

    @MainThread
    public int getCurrentBookIndex() {
        return audioBooks.indexOf(currentBook);
    }

    @MainThread
    @Nullable
    public AudioBook getById(String id) {
        for (AudioBook book : audioBooks)
            if (book.getId().equals(id))
                return book;
        return null;
    }

    @MainThread
    public File getDefaultAudioBooksDirectory() {
        return fileScanner.getDefaultAudioBooksDirectory();
    }

    @MainThread
    public boolean isInitialized() {
        return isInitialized;
    }

    @MainThread
    public void scanFiles() {
        SimpleFuture<List<FileSet>> future = fileScanner.scanAudioBooksDirectories();
        future.addListener(new SimpleFuture.Listener<List<FileSet>>() {
            @Override
            public void onResult(@NonNull List<FileSet> result) {
                isInitialized = true;
                processScanResult(result);
            }

            @Override
            public void onException(@NonNull Throwable t) {
                isInitialized = true;
                // TODO: clear the list of books?
                CrashReporting.logException(t);
            }
        });
    }

    @MainThread
    private void processScanResult(@NonNull List<FileSet> fileSets) {
        if (isFirstScan && fileSets.isEmpty()) {
            // The first scan may fail if it is just after booting and the SD card is not yet
            // mounted. Retry in a while.
            Handler handler = new Handler(Looper.myLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanFiles();
                }
            }, TimeUnit.SECONDS.toMillis(10));
        }

        // This isn't very efficient but there shouldn't be more than a dozen audio books on the
        // device.
        List<AudioBook> booksToRemove = new ArrayList<>();
        for (AudioBook audioBook : audioBooks) {
            String id = audioBook.getId();
            boolean isInFileSet = false;
            for (FileSet fileSet : fileSets) {
                if (id.equals(fileSet.id)) {
                    isInFileSet = true;
                    break;
                }
            }
            if (!isInFileSet)
                booksToRemove.add(audioBook);
        }
        if (booksToRemove.contains(currentBook))
            currentBook = null;
        boolean audioBooksChanged = audioBooks.removeAll(booksToRemove);
        LibraryContentType contentType = LibraryContentType.EMPTY;

        for (FileSet fileSet : fileSets) {
            if (getById(fileSet.id) == null) {
                AudioBook audioBook = new AudioBook(fileSet);
                storage.readAudioBookState(audioBook);
                audioBook.setUpdateObserver(storage);
                audioBooks.add(audioBook);
                audioBooksChanged = true;
            }
            LibraryContentType newContentType = fileSet.isDemoSample
                    ? LibraryContentType.SAMPLES_ONLY : LibraryContentType.USER_CONTENT;
            if (newContentType.supersedes(contentType))
                contentType = newContentType;
        }

        if (audioBooks.size() > 0) {
            Collections.sort(audioBooks, new Comparator<AudioBook>() {
                @Override
                public int compare(AudioBook lhs, AudioBook rhs) {
                    return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
                }
            });

            assignColoursToNewBooks();
        }
        if (currentBook == null) {
            String id = storage.getCurrentAudioBook();
            if (getById(id) == null && audioBooks.size() > 0)
                id = audioBooks.get(0).getId();

            if (id != null)
                setCurrentBook(id);
        }

        if (audioBooksChanged || isFirstScan)
            eventBus.post(new AudioBooksChangedEvent(contentType));

        isFirstScan = false;
    }

    @MainThread
    public void resetAllBookProgress() {
        for (AudioBook book : audioBooks) {
            book.resetPosition();
        }
    }

    @MainThread
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
                storage.writeAudioBookState(currentBook);
            }
        }
    }

    @MainThread
    @NonNull
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
