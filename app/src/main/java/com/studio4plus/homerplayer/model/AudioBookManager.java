package com.studio4plus.homerplayer.model;

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
    public AudioBookManager(EventBus eventBus, FileScanner fileScanner, Storage storage) {
        this.fileScanner = fileScanner;
        this.storage = storage;
        scanFiles();
        eventBus.register(this);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEvent(MediaStoreUpdateEvent ignored) {
        scanFiles();
    }

    public List<AudioBook> getAudioBooks() {
        return audioBooks;
    }

    public void setCurrentBook(String bookId) {
        AudioBook newBook = getById(bookId);
        if (newBook != currentBook) {
            currentBook = getById(bookId);
            EventBus.getDefault().post(new CurrentBookChangedEvent(currentBook));
        }
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

    public File getAbsolutePath(AudioBook book) {
        return new File(fileScanner.getAudioBooksDirectory(), book.getDirectoryName());
    }

    public File getAudioBooksDirectory() {
        return fileScanner.getAudioBooksDirectory();
    }

    public void scanFiles() {
        boolean audioBooksChanged;
        List<FileSet> fileSets = fileScanner.scanAudioBooksDirectory();

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

        if (fileSets != null) {
            for (FileSet fileSet : fileSets) {
                if (getById(fileSet.id) == null) {
                    AudioBook audioBook = new AudioBook(fileSet);
                    storage.readAudioBookState(audioBook);
                    audioBook.setPositionObserver(storage);
                    audioBooks.add(audioBook);
                    audioBooksChanged = true;
                }
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
            EventBus.getDefault().post(new AudioBooksChangedEvent(currentBook != null));
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
                storage.writeAudioBookState(currentBook);
            }
        }
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
