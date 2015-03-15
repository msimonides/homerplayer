package com.studio4plus.audiobookplayer.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.studio4plus.audiobookplayer.model.AudioBook;
import com.studio4plus.audiobookplayer.model.AudioBookManager;

public class FragmentWithBook extends Fragment {

    private static final String ARG_DIRECTORY_NAME = "directoryName";

    protected AudioBook audioBook;

    public static void setArgDirectoryName(FragmentWithBook fragment, String directoryName) {
        Bundle args = new Bundle();
        args.putString(ARG_DIRECTORY_NAME, directoryName);
        fragment.setArguments(args);
    }

    public AudioBook getAudioBook() {
        if (audioBook == null) {
            audioBook = AudioBookManager.getInstance().get(
                    getArguments().getString(ARG_DIRECTORY_NAME));
        }
        return audioBook;
    }
}
