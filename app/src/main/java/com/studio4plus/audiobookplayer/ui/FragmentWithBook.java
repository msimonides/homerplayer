package com.studio4plus.audiobookplayer.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.studio4plus.audiobookplayer.AudioBookPlayerApplication;
import com.studio4plus.audiobookplayer.model.AudioBook;
import com.studio4plus.audiobookplayer.model.AudioBookManager;

public class FragmentWithBook extends Fragment {

    private static final String ARG_BOOK_ID = "bookId";

    protected AudioBook audioBook;

    public static void setArgBookId(FragmentWithBook fragment, String bookId) {
        Bundle args = new Bundle();
        args.putString(ARG_BOOK_ID, bookId);
        fragment.setArguments(args);
    }

    public AudioBook getAudioBook() {
        if (audioBook == null) {
            audioBook = AudioBookPlayerApplication.getAudioBookManager().getById(
                    getArguments().getString(ARG_BOOK_ID));
        }
        return audioBook;
    }
}
