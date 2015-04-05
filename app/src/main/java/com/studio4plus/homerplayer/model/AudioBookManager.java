package com.studio4plus.homerplayer.model;

import com.studio4plus.homerplayer.events.CurrentBookChangedEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class AudioBookManager {

    private final List<AudioBook> audioBooks = new ArrayList<>();
    private final FileScanner fileScanner;
    private final Storage storage;
    private AudioBook currentBook;

    public AudioBookManager(FileScanner fileScanner, Storage storage) {
        this.fileScanner = fileScanner;
        this.storage = storage;
        scanFiles();
    }

    public List<AudioBook> getAudioBooks() {
        return audioBooks;
    }

    public void setCurrentBook(AudioBook book) {
        currentBook = book;
        EventBus.getDefault().post(new CurrentBookChangedEvent(book));
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
        List<FileSet> fileSets = fileScanner.scanAudioBooksDirectory();
        audioBooks.clear();
        if (fileSets != null) {
            for (FileSet fileSet : fileSets) {
                AudioBook audioBook = new AudioBook(fileSet);
                storage.readAudioBookState(audioBook);
                audioBook.setPositionObserver(storage);
                audioBooks.add(audioBook);
            }
        }

        if (audioBooks.size() > 0) {
            assignColoursToNewBooks();

            String id = storage.getCurrentAudioBook();
            currentBook = getById(id);
            if (currentBook == null)
                currentBook = audioBooks.get(0);
        }
        // TODO: refresh UI.
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
