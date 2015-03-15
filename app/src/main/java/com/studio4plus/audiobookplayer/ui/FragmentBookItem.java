package com.studio4plus.audiobookplayer.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.studio4plus.audiobookplayer.R;
import com.studio4plus.audiobookplayer.model.AudioBook;

public class FragmentBookItem extends FragmentWithBook {

    public static FragmentBookItem newInstance(String directoryName) {
        FragmentBookItem newFragment = new FragmentBookItem();
        setArgDirectoryName(newFragment, directoryName);
        return newFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_item, container, false);

        AudioBook audioBook = getAudioBook();
        TextView textView = (TextView) view.findViewById(R.id.title);
        textView.setText(audioBook.getTitle());

        return view;
    }

}
