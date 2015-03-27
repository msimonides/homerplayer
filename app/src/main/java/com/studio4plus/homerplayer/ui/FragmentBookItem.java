package com.studio4plus.homerplayer.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.model.AudioBook;

public class FragmentBookItem extends FragmentWithBook {

    public static FragmentBookItem newInstance(String bookId) {
        FragmentBookItem newFragment = new FragmentBookItem();
        setArgBookId(newFragment, bookId);
        return newFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_item, container, false);

        AudioBook audioBook = getAudioBook();
        TextView textView = (TextView) view.findViewById(R.id.title);
        textView.setText(audioBook.getTitle());
        textView.setTextColor(audioBook.getColourScheme().textColour);

        view.setBackgroundColor(audioBook.getColourScheme().backgroundColour);

        return view;
    }

}
