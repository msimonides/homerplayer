package com.studio4plus.homerplayer.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.model.AudioBook;

public class FragmentBookItem extends BookListChildFragment {

    public static FragmentBookItem newInstance(AudioBook book) {
        FragmentBookItem newFragment = new FragmentBookItem();
        Bundle args = new Bundle();
        bookToBundle(args, book);
        newFragment.setArguments(args);
        return newFragment;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_item, container, false);

        Bundle args = getArguments();
        final String bookId = args.getString(ARG_BOOK_ID);
        if (bookId != null) {
            TextView textView = (TextView) view.findViewById(R.id.title);
            textView.setText(args.getString(ARG_BOOK_TITLE));
            textView.setTextColor(args.getInt(ARG_BOOK_TEXT_COLOR));
            view.setBackgroundColor(args.getInt(ARG_BOOK_BACKGROUND_COLOR));

            Button startButton = (Button) view.findViewById(R.id.startButton);
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.startPlayback(bookId);
                }
            });
        }

        return view;
    }

    public String getAudioBookId() {
        return getArguments().getString(ARG_BOOK_ID);
    }

    private static final String ARG_BOOK_ID = "bookId";
    private static final String ARG_BOOK_TITLE = "bookTitle";
    private static final String ARG_BOOK_TEXT_COLOR = "bookTextColor";
    private static final String ARG_BOOK_BACKGROUND_COLOR = "bookBackgroundColor";

    protected static void bookToBundle(Bundle bundle, AudioBook audioBook) {
        bundle.putString(ARG_BOOK_ID, audioBook.getId());
        bundle.putString(ARG_BOOK_TITLE, audioBook.getTitle());
        bundle.putInt(ARG_BOOK_TEXT_COLOR, audioBook.getColourScheme().textColour);
        bundle.putInt(ARG_BOOK_BACKGROUND_COLOR, audioBook.getColourScheme().backgroundColour);
    }
}
