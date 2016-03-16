package com.studio4plus.homerplayer.ui;

import android.app.Fragment;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.HomerPlayerApplication;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.events.PlaybackElapsedTimeSyncEvent;
import com.studio4plus.homerplayer.events.PlaybackStoppingEvent;
import com.studio4plus.homerplayer.util.ViewUtils;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

public class FragmentPlayback extends Fragment {

    private View view;
    private Button stopButton;
    private TextView elapsedTimeView;
    private TextView elapsedTimeRewindFFView;
    private TimerTask timerTask;

    @Inject public GlobalSettings globalSettings;
    @Inject public EventBus eventBus;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_playback, container, false);
        HomerPlayerApplication.getComponent(view.getContext()).inject(this);

        stopButton = (Button) view.findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMainActivity().stopPlayback();
            }
        });

        elapsedTimeView = (TextView) view.findViewById(R.id.elapsedTime);
        elapsedTimeRewindFFView = (TextView) view.findViewById(R.id.elapsedTimeRewindFF);

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

        View rewindFFOverlay = view.findViewById(R.id.rewindFFOverlay);
        RewindFFHandler rewindFFHandler = new RewindFFHandler(
                (View) rewindFFOverlay.getParent(), rewindFFOverlay);
        view.findViewById(R.id.rewindButton).setOnTouchListener(new PressReleaseDetector(rewindFFHandler));
        view.findViewById(R.id.fastForwardButton).setOnTouchListener(new PressReleaseDetector(rewindFFHandler));

        rewindFFOverlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Don't let any events "through" the overlay.
                return true;
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        showHintIfNecessary();
        eventBus.register(this);
        getMainActivity().requestElapsedTimeSyncEvent();
    }

    @Override
    public void onPause() {
        if (timerTask != null)
            timerTask.stop();
        eventBus.unregister(this);
        super.onPause();
    }

    @SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
    public void onEvent(PlaybackStoppingEvent event) {
        if (stopButton != null)
            stopButton.setEnabled(false);
        if (timerTask != null) {
            timerTask.stop();
            timerTask = null;
        }
    }

    @SuppressWarnings({"UnusedParameters", "UnusedDeclaration"})
    public void onEvent(PlaybackElapsedTimeSyncEvent event) {
        if (timerTask != null)
            timerTask.stop();

        timerTask = new TimerTask(new Handler(Looper.myLooper()), event.playbackPositionMs);
        timerTask.run();
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

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    private static final long TIMER_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1);

    private class TimerTask implements Runnable {

        private final Handler handler;
        private final long startTimeMs;
        private final long baseDisplayTimeMs;

        private TimerTask(Handler handler, long baseDisplayTimeMs) {
            this.handler = handler;
            this.baseDisplayTimeMs = baseDisplayTimeMs;
            this.startTimeMs = System.currentTimeMillis();
        }

        public long getCurrentElapsedTimeMs() {
            return baseDisplayTimeMs + (System.currentTimeMillis() - startTimeMs);
        }

        @Override
        public void run() {
            elapsedTimeView.setText(elapsedTime(getCurrentElapsedTimeMs()));

            long adjustmentMs = (System.currentTimeMillis() - startTimeMs) % TIMER_INTERVAL_MS;
            handler.postDelayed(this, TIMER_INTERVAL_MS - adjustmentMs);
        }

        public void stop() {
            handler.removeCallbacks(this);
        }
    }

    private class RewindFFHandler implements PressReleaseDetector.Listener {

        private final View commonParent;
        private final View rewindOverlay;
        private SupportAnimator currentAnimator;

        private RewindFFHandler(@NonNull View commonParent, @NonNull View rewindOverlay) {
            this.commonParent = commonParent;
            this.rewindOverlay = rewindOverlay;
        }

        @Override
        public void onPressed(View v, float x, float y) {
            Preconditions.checkNotNull(timerTask);
            if (currentAnimator != null) {
                currentAnimator.cancel();
            }

            elapsedTimeRewindFFView.setText(elapsedTime(timerTask.getCurrentElapsedTimeMs()));
            rewindOverlay.setVisibility(View.VISIBLE);
            currentAnimator = createAnimation(v, x, y, true);
            currentAnimator.addListener(new SupportAnimator.SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd() {
                    currentAnimator = null;
                }
            });
            currentAnimator.start();
        }

        @Override
        public void onReleased(View v, float x, float y) {
            if (currentAnimator != null) {
                currentAnimator.cancel();
                rewindOverlay.setVisibility(View.GONE);
                currentAnimator = null;
            } else {
                currentAnimator = createAnimation(v, x, y, false);
                currentAnimator.addListener(new SupportAnimator.SimpleAnimatorListener() {
                    @Override
                    public void onAnimationEnd() {
                        rewindOverlay.setVisibility(View.GONE);
                        currentAnimator = null;
                    }
                });
                currentAnimator.start();
            }
        }

        private SupportAnimator createAnimation(View v, float x, float y, boolean reveal) {
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
            SupportAnimator animator = ViewAnimationUtils.createCircularReveal(
                    rewindOverlay, Math.round(startX), Math.round(startY), initialRadius, finalRadius);
            animator.setDuration(getResources().getInteger(durationResId));
            animator.setInterpolator(new AccelerateDecelerateInterpolator());

            return animator;
        }
    }
}
