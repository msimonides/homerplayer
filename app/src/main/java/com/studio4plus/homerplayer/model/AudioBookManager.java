package com.studio4plus.homerplayer.model;

import android.support.annotation.MainThread;

import com.studio4plus.homerplayer.events.AudioBooksChangedEvent;
import com.studio4plus.homerplayer.events.CurrentBookChangedEvent;
import com.studio4plus.homerplayer.events.MediaStoreUpdateEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;

@Singleton
public class AudioBookManager {

    private final List<AudioBook> audioBooks = new ArrayList<>();
    private final FileScanner fileScanner;
    private final Storage storage;
    private AudioBook currentBook;

    @Inject
    @MainThread
    public AudioBookManager(EventBus eventBus, FileScanner fileScanner, Storage storage) {
        this.fileScanner = fileScanner;
        this.storage = storage;
        scanFiles();
        eventBus.register(this);
    }

    @SuppressWarnings("UnusedDeclaration")
    @MainThread
    public void onEvent(MediaStoreUpdateEvent ignored) {
        scanFiles();
    }

    @MainThread
    public List<AudioBook> getAudioBooks() {
        return audioBooks;
    }

    @MainThread
    public void setCurrentBook(String bookId) {
        AudioBook newBook = getById(bookId);
        if (newBook != currentBook) {
            currentBook = getById(bookId);
            EventBus.getDefault().post(new CurrentBookChangedEvent(currentBook));
        }
    }

    @MainThread
    public AudioBook getCurrentBook() {
        return currentBook;
    }

    @MainThread
    public int getCurrentBookIndex() {
        return audioBooks.indexOf(currentBook);
    }

    @MainThread
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
    public void scanFiles() {
        boolean audioBooksChanged;
        List<FileSet> fileSets = fileScanner.scanAudioBooksDirectories();

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
        audioBooksChanged = audioBooks.removeAll(booksToRemove);
        LibraryContentType contentType = LibraryContentType.EMPTY;

        if (fileSets != null) {
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

        if (audioBooksChanged)
            EventBus.getDefault().post(new AudioBooksChangedEvent(contentType));
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
