package com.studio4plus.homerplayer.events;

import com.studio4plus.homerplayer.model.LibraryContentType;

/**
 * Posted when audio books are added or removed.
 */
public class AudioBooksChangedEvent {

    public final LibraryContentType contentType;

    public AudioBooksChangedEvent(LibraryContentType contentType) {
        this.contentType = contentType;
    }
}
