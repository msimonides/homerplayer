package com.studio4plus.homerplayer.ui;

import android.support.annotation.NonNull;

/**
 * The main UI part that handles switching between the main states: no books, list of books,
 * book playback.
 */
public interface MainUi {

    @NonNull BookListUi switchToBookList(boolean animate);
    @NonNull NoBooksUi switchToNoBooks(boolean animate);
    @NonNull PlaybackUi switchToPlayback(boolean animate);
}
