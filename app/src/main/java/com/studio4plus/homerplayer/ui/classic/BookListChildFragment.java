package com.studio4plus.homerplayer.ui.classic;

import android.animation.Animator;
import android.animation.ValueAnimator;
import androidx.fragment.app.Fragment;

import com.studio4plus.homerplayer.R;

/**
 * A class implementing a workaround for https://code.google.com/p/android/issues/detail?id=55228
 *
 * Inspired by http://stackoverflow.com/a/23276145/3892517
 */
public class BookListChildFragment extends Fragment {

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        final Fragment parent = getParentFragment();

        // Apply the workaround only if this is a child fragment, and the parent
        // is being removed.
        if (!enter && parent != null && parent.isRemoving()) {
            ValueAnimator nullAnimation = new ValueAnimator();
            nullAnimation.setIntValues(1, 1);
            nullAnimation.setDuration(R.integer.flip_animation_time_half_ms);
            return nullAnimation;
        } else {
            return super.onCreateAnimator(transit, enter, nextAnim);
        }
    }
}
