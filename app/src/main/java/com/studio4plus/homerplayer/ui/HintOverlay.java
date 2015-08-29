package com.studio4plus.homerplayer.ui;

import android.view.View;
import android.view.ViewStub;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import com.studio4plus.homerplayer.R;

public class HintOverlay {

    private final View parentView;
    private final int viewStubId;
    private final int textResourceId;
    private final int imageResourceId;

    public HintOverlay(View parentView, int viewStubId, int textResourceId, int imageResourceId) {
        this.parentView = parentView;
        this.viewStubId = viewStubId;
        this.textResourceId = textResourceId;
        this.imageResourceId = imageResourceId;
    }

    public void show() {
        ViewStub stub = (ViewStub) parentView.findViewById(viewStubId);
        if (stub != null) {
            final View hintOverlay = stub.inflate();
            hintOverlay.setVisibility(View.VISIBLE);

            ((ImageView) hintOverlay.findViewById(R.id.image)).setImageResource(imageResourceId);
            ((TextView) hintOverlay.findViewById(R.id.text)).setText(
                    parentView.getResources().getString(textResourceId));

            Animation animation = new AlphaAnimation(0, 1);
            animation.setDuration(750);
            animation.setStartOffset(500);

            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    hintOverlay.setOnClickListener(new HideHintClickListener(hintOverlay));
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            hintOverlay.startAnimation(animation);
            hintOverlay.setOnClickListener(new BlockClickListener());
        }
    }

    private static class BlockClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // Do nothing.
        }
    }

    private static class HideHintClickListener implements View.OnClickListener {

        private final View hintOverlay;

        HideHintClickListener(View hintOverlay) {
            this.hintOverlay = hintOverlay;
        }

        @Override
        public void onClick(View v) {
            Animation animation = new AlphaAnimation(1, 0);
            animation.setDuration(300);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    hintOverlay.setOnClickListener(null);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    hintOverlay.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            hintOverlay.startAnimation(animation);
        }
    }


}
