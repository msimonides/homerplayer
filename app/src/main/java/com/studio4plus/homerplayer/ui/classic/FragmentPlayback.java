package com.studio4plus.homerplayer.ui.classic;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.crashreporting.CrashReporting;
import com.studio4plus.homerplayer.ui.FFRewindTimer;
import com.studio4plus.homerplayer.ui.HintOverlay;
import com.studio4plus.homerplayer.ui.PressReleaseDetector;
import com.studio4plus.homerplayer.ui.RepeatButton;
import com.studio4plus.homerplayer.ui.SimpleAnimatorListener;
import com.studio4plus.homerplayer.ui.UiControllerPlayback;
import com.studio4plus.homerplayer.util.ViewUtils;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.codetail.animation.ViewAnimationUtils;

public class FragmentPlayback extends Fragment implements FFRewindTimer.Observer {

    private View view;
    private Button stopButton;
    private ImageButton rewindButton;
    private ImageButton ffButton;
    private TextView elapsedTimeView;
    private TextView elapsedTimeRewindFFView;
    private VolumeIndicatorShowController volumeIndicatorShowController;
    private VolumeChangeIndicator volumeIndicator;
    private RewindFFHandler rewindFFHandler;
    private Animator elapsedTimeRewindFFViewAnimation;

    private @Nullable UiControllerPlayback controller;

    @Inject public GlobalSettings globalSettings;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_playback, container, false);
        HomerPlayerApplication.getComponent(view.getContext()).inject(this);

        stopButton = view.findViewById(R.id.stopButton);
        stopButton.setOnClickListener(v -> {
            Preconditions.checkNotNull(controller);
            controller.stopPlayback();
        });

        elapsedTimeView = view.findViewById(R.id.elapsedTime);
        elapsedTimeRewindFFView = view.findViewById(R.id.elapsedTimeRewindFF);

        elapsedTimeView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                    View v,
                    int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                RelativeLayout.LayoutParams params =
                        (RelativeLayout.LayoutParams) elapsedTimeRewindFFView.getLayoutParams();
                params.leftMargin = left;
                params.topMargin = top;
                params.width = right - left;
                params.height = bottom - top;
                elapsedTimeRewindFFView.setLayoutParams(params);
            }
        });

        // Don't let any events "through" overlays.
        View.OnTouchListener capturingListener = (v, event) -> true;
        RepeatButton volumeUp = view.findViewById(R.id.volumeUp);
        RepeatButton volumeDown = view.findViewById(R.id.volumeDown);
        if (globalSettings.isVolumeControlEnabled()) {
            volumeUp.setOnClickListener(this::volumeUp);
            volumeUp.setOnLongClickListener(this::volumeUp);
            volumeUp.setOnPressListener(this::requestShowVolume);
            volumeDown.setOnClickListener(this::volumeDown);
            volumeDown.setOnLongClickListener(this::volumeDown);
            volumeDown.setOnPressListener(this::requestShowVolume);

            View volumeIndicatorOverlay = view.findViewById(R.id.volumeIndicatorOverlay);
            volumeIndicatorOverlay.setOnTouchListener(capturingListener);
            volumeIndicatorShowController = new VolumeIndicatorShowController(volumeIndicatorOverlay);
            volumeIndicator = view.findViewById(R.id.volumeIndicator);
        } else {
            volumeUp.setVisibility(View.GONE);
            volumeDown.setVisibility(View.GONE);
        }

        rewindButton = view.findViewById(R.id.rewindButton);
        ffButton = view.findViewById(R.id.fastForwardButton);

        View rewindFFOverlay = view.findViewById(R.id.rewindFFOverlay);
        rewindFFHandler = new RewindFFHandler(
                (View) rewindFFOverlay.getParent(), rewindFFOverlay);
        rewindButton.setEnabled(false);
        ffButton.setEnabled(false);
        rewindFFOverlay.setOnTouchListener(capturingListener);

        elapsedTimeRewindFFViewAnimation =
                AnimatorInflater.loadAnimator(view.getContext(), R.animator.bounce);
        elapsedTimeRewindFFViewAnimation.setTarget(elapsedTimeRewindFFView);

        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onResume() {
        super.onResume();
        CrashReporting.log("UI: FragmentPlayback resumed");
        rewindButton.setOnTouchListener(new PressReleaseDetector(rewindFFHandler));
        ffButton.setOnTouchListener(new PressReleaseDetector(rewindFFHandler));
        showHintIfNecessary();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onPause() {
        // Remove press-release detectors and tell rewindFFHandler directly that we're paused.
        rewindButton.setOnTouchListener(null);
        ffButton.setOnTouchListener(null);
        rewindFFHandler.onPause();
        super.onPause();
    }

    void onPlaybackStopping() {
        disableUiOnStopping();
        rewindFFHandler.onStopping();
    }

    void onPlaybackProgressed(long playbackPositionMs) {
        onTimerUpdated(playbackPositionMs);
        enableUiOnStart();
    }

    void onVolumeChanged(int min, int max, int current) {
        volumeIndicator.setVolume(min, max, current);
        volumeIndicatorShowController.show();
    }

    void enableUiOnStart() {
        rewindButton.setEnabled(true);
        ffButton.setEnabled(true);
    }

    private void disableUiOnStopping() {
        rewindButton.setEnabled(false);
        stopButton.setEnabled(false);
        ffButton.setEnabled(false);
    }

    private boolean volumeUp(@NonNull View ignored) {
        Preconditions.checkNotNull(controller);
        controller.volumeUp();
        return true;
    }

    private boolean volumeDown(@NonNull View ignored) {
        Preconditions.checkNotNull(controller);
        controller.volumeDown();
        return true;
    }

    private void requestShowVolume(@NonNull View ignored) {
        Preconditions.checkNotNull(controller);
        controller.showVolume();
    }

    private static class VolumeIndicatorShowController {
        private static final long SHOW_HIDE_DURATION_MS = 500;
        private static final long AUTO_HIDE_DURATION_MS = 3000;

        @NonNull
        private final View volumeIndicatorOverlay;
        private boolean isShown = false;

        @NonNull
        private final Runnable hideTask = this::hide;

        private VolumeIndicatorShowController(@NonNull View volumeIndicatorOverlay) {
            this.volumeIndicatorOverlay = volumeIndicatorOverlay;
        }

        void show() {
            if (!isShown) {
                volumeIndicatorOverlay.animate().cancel();
                volumeIndicatorOverlay.setVisibility(View.VISIBLE);
                long duration = Math.round(
                        (1f - volumeIndicatorOverlay.getAlpha()) * SHOW_HIDE_DURATION_MS);
                volumeIndicatorOverlay.animate()
                        .alpha(1f)
                        .setDuration(duration)
                        .setListener(null)
                        .start();
                isShown = true;
            }
            volumeIndicatorOverlay.removeCallbacks(hideTask);
            volumeIndicatorOverlay.postDelayed(hideTask, AUTO_HIDE_DURATION_MS);
        }

        void hide() {
            isShown = false;
            volumeIndicatorOverlay.removeCallbacks(hideTask);
            volumeIndicatorOverlay.animate().cancel();
            long duration = Math.round(
                    volumeIndicatorOverlay.getAlpha() * SHOW_HIDE_DURATION_MS);
            volumeIndicatorOverlay.animate()
                    .alpha(0f)
                    .setDuration(duration)
                    .setListener(new SimpleAnimatorListener() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            volumeIndicatorOverlay.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }

    private String elapsedTime(long elapsedMs) {
        long hours = TimeUnit.MILLISECONDS.toHours(elapsedMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMs) % 60;

        return getString(R.string.playback_elapsed_time, hours, minutes, seconds);
    }

    private void showHintIfNecessary() {
        if (isResumed() && isVisible()) {
            if (!globalSettings.flipToStopHintShown()) {
                HintOverlay overlay = new HintOverlay(
                        view, R.id.flipToStopHintOverlayStub, R.string.hint_flip_to_stop, R.drawable.hint_flip_to_stop);
                overlay.show();
                globalSettings.setFlipToStopHintShown();
            }
        }
    }

    @Override
    public void onTimerUpdated(long displayTimeMs) {
        elapsedTimeView.setText(elapsedTime(displayTimeMs));
        elapsedTimeRewindFFView.setText(elapsedTime(displayTimeMs));
    }

    @Override
    public void onTimerLimitReached() {
        if (elapsedTimeRewindFFView.getVisibility() == View.VISIBLE) {
            elapsedTimeRewindFFViewAnimation.start();
        }
    }

    void setController(@NonNull UiControllerPlayback controller) {
        this.controller = controller;
    }

    private class RewindFFHandler implements PressReleaseDetector.Listener {

        private final View commonParent;
        private final View rewindOverlay;
        private Animator currentAnimator;
        private boolean isRunning;

        private RewindFFHandler(@NonNull View commonParent, @NonNull View rewindOverlay) {
            this.commonParent = commonParent;
            this.rewindOverlay = rewindOverlay;
        }

        @Override
        public void onPressed(final View v, float x, float y) {
            Preconditions.checkNotNull(controller);
            if (currentAnimator != null) {
                currentAnimator.cancel();
            }

            final boolean isFF = (v == ffButton);
            rewindOverlay.setVisibility(View.VISIBLE);
            currentAnimator = createAnimation(v, x, y, true);
            currentAnimator.addListener(new AnimatorListener() {
                @Override
                protected void onAnimationCompleted() {
                    controller.startRewind(isFF, FragmentPlayback.this);
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    super.onAnimationCancel(animator);
                    resumeFromRewind();
                }

                @Override
                protected void onCompletedOrCancelled() {
                    currentAnimator = null;
                }
            });
            currentAnimator.start();

            controller.pauseForRewind();
            isRunning = true;
        }

        @Override
        public void onReleased(View v, float x, float y) {
            if (currentAnimator != null) {
                currentAnimator.cancel();
                rewindOverlay.setVisibility(View.GONE);
                currentAnimator = null;
            } else {
                currentAnimator = createAnimation(v, x, y, false);
                currentAnimator.addListener(new AnimatorListener() {
                    @Override
                    protected void onCompletedOrCancelled() {
                        rewindOverlay.setVisibility(View.GONE);
                        currentAnimator = null;
                    }
                });
                currentAnimator.start();
                resumeFromRewind();
            }
        }

        void onPause() {
            if (currentAnimator != null) {
                // Cancelling the animation calls resumeFromRewind.
                currentAnimator.cancel();
                currentAnimator = null;
            } else if (isRunning) {
                resumeFromRewind();
            }
        }

        void onStopping() {
            if (isRunning)
                stopRewind();
        }

        private void resumeFromRewind() {
            Preconditions.checkNotNull(controller);
            stopRewind();
            controller.resumeFromRewind();
        }

        private void stopRewind() {
            Preconditions.checkNotNull(controller);
            controller.stopRewind();
            isRunning = false;
        }

        private Animator createAnimation(View v, float x, float y, boolean reveal) {
            Rect viewRect = ViewUtils.getRelativeRect(commonParent, v);
            float startX = viewRect.left + x;
            float startY = viewRect.top + y;

            // Compute final radius
            float dx = Math.max(startX, commonParent.getWidth() - startX);
            float dy = Math.max(startY, commonParent.getHeight() - startY);
            float finalRadius = (float) Math.hypot(dx, dy);

            float initialRadius = reveal ? 0f : finalRadius;
            if (!reveal)
                finalRadius = 0f;

            final int durationResId = reveal
                    ? R.integer.ff_rewind_overlay_show_animation_time_ms
                    : R.integer.ff_rewind_overlay_hide_animation_time_ms;
            Animator animator = ViewAnimationUtils.createCircularReveal(
                    rewindOverlay, Math.round(startX), Math.round(startY), initialRadius, finalRadius);
            animator.setDuration(getResources().getInteger(durationResId));
            animator.setInterpolator(new AccelerateDecelerateInterpolator());

            return animator;
        }
    }

    private static abstract class AnimatorListener extends SimpleAnimatorListener {
        private boolean isCancelled = false;

        @Override
        public final void onAnimationEnd(Animator animator) {
            if (!isCancelled) {
                onAnimationCompleted();
                onCompletedOrCancelled();
            }
        }

        @CallSuper
        @Override
        public void onAnimationCancel(Animator animator) {
            isCancelled = true;
            onCompletedOrCancelled();
        }

        protected abstract void onCompletedOrCancelled();
        protected void onAnimationCompleted() {}
    }
}
