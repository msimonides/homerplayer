package com.studio4plus.homerplayer.events;

/**
 * Posted when audio books are added or removed.
 */
public class AudioBooksChangedEvent {

    public final boolean hasBooks;

    public AudioBooksChangedEvent(boolean hasBooks) {
        this.hasBooks = hasBooks;
    }
}
