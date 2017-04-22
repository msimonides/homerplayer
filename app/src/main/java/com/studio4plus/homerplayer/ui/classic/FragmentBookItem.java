package com.studio4plus.homerplayer.ui.classic;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.model.AudioBookManager;
import com.studio4plus.homerplayer.ui.MainActivity;
import com.studio4plus.homerplayer.ui.UiControllerBookList;

import javax.inject.Inject;
import javax.inject.Named;

public class FragmentBookItem extends BookListChildFragment {

    public static FragmentBookItem newInstance(String bookId) {
        FragmentBookItem newFragment = new FragmentBookItem();
        Bundle args = new Bundle();
        args.putString(ARG_BOOK_ID, bookId);
        newFragment.setArguments(args);
        return newFragment;
    }

    @Inject public AudioBookManager audioBookManager;
    @Inject @Named("AUDIOBOOKS_DIRECTORY") public String audioBooksDirectoryName;

    private @Nullable UiControllerBookList controller;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_book_item, container, false);
        HomerPlayerApplication.getComponent(view.getContext()).inject(this);

        Bundle args = getArguments();
        final String bookId = args.getString(ARG_BOOK_ID);
        if (bookId != null) {
            AudioBook book = audioBookManager.getById(bookId);
            TextView textView = (TextView) view.findViewById(R.id.title);
            textView.setText(book.getTitle());
            textView.setTextColor(book.getColourScheme().textColour);
            view.setBackgroundColor(book.getColourScheme().backgroundColour);

            if (book.isDemoSample()) {
                TextView copyBooksInstruction =
                        (TextView) view.findViewById(R.id.copyBooksInstruction);
                String directoryMessage =
                        getString(R.string.copyBooksInstructionMessage, audioBooksDirectoryName);
                copyBooksInstruction.setText(Html.fromHtml(directoryMessage));
                copyBooksInstruction.setTextColor(book.getColourScheme().textColour);
                copyBooksInstruction.setVisibility(View.VISIBLE);
            }

            final Button startButton = (Button) view.findViewById(R.id.startButton);
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Preconditions.checkNotNull(controller);
                    controller.playCurrentAudiobook();
                    startButton.setEnabled(false);
                }
            });
        }

        return view;
    }

    public String getAudioBookId() {
        return getArguments().getString(ARG_BOOK_ID);
    }

    void setController(@NonNull UiControllerBookList controller) {
        this.controller = controller;
    }

    private static final String ARG_BOOK_ID = "bookId";
}
