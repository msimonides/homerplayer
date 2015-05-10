package com.studio4plus.homerplayer.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.model.AudioBookManager;

import javax.inject.Inject;

public class FragmentNoBooks extends Fragment {

    @Inject public AudioBookManager audioBookManager;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_no_books, container, false);
        HomerPlayerApplication.getComponent(view.getContext()).inject(this);

        TextView noBooksPath = (TextView) view.findViewById(R.id.noBooksPath);
        noBooksPath.setText(audioBookManager.getAudioBooksDirectory().getAbsolutePath());

        return view;
    }

}
