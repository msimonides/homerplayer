package com.studio4plus.homerplayer.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.model.AudioBook;
import com.studio4plus.homerplayer.model.AudioBookManager;

public class FragmentPlayback extends Fragment implements AudioBookManager.Listener {

    private TextView titleTextView;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playback, container, false);

        AudioBookManager audioBookManager = HomerPlayerApplication.getAudioBookManager();

        titleTextView = (TextView) view.findViewById(R.id.title);
        if (audioBookManager.getCurrentBook() != null)
            titleTextView.setText(audioBookManager.getCurrentBook().getTitle());

        audioBookManager.addWeakListener(this);

        return view;
    }

    @Override
    public void onCurrentBookChanged(AudioBook book) {
        if (titleTextView != null)
            titleTextView.setText(book.getTitle());
    }
}
