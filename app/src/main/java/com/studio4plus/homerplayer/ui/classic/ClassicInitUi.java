package com.studio4plus.homerplayer.ui.classic;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import androidx.fragment.app.Fragment;

import com.studio4plus.homerplayer.R;

import java.util.Objects;

public class ClassicInitUi extends Fragment {

    public ClassicInitUi() {
        super(R.layout.fragment_init);
    }

    @Override
    public void onStart() {
        super.onStart();
        View logo = requireView().findViewById(R.id.logo);
        logo.setAlpha(0f);
        logo.setScaleX(0.8f);
        logo.setScaleY(0.8f);
        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1500)
                .setInterpolator(new AccelerateDecelerateInterpolator());
    }
}
