package com.studio4plus.homerplayer.ui;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.Toast;

import com.studio4plus.homerplayer.R;

import java.util.concurrent.TimeUnit;

public class MultitapGestureDetectorListener extends GestureDetector.SimpleOnGestureListener {

    public interface Listener {
        void onMultiTap();
    }

    private static final int TRIGGER_TAP_COUNT = 5;

    private final Context context;

    private Listener listener;
    private long lastTouchDownNanoTime;
    private float lastTouchDownX;
    private float lastTouchDownY;
    private int consecutiveTapCount;

    private int maxTapSlop;
    private int maxMultiTapSlop;
    private long maxTapNanoTime;
    private long maxConsecutiveTapNanoTime;
    private Toast lastToast;

    public MultitapGestureDetectorListener(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;

        ViewConfiguration configuration = ViewConfiguration.get(context);
        maxTapSlop = configuration.getScaledTouchSlop();
        maxMultiTapSlop = configuration.getScaledDoubleTapSlop();
        maxTapNanoTime = TimeUnit.MILLISECONDS.toNanos(ViewConfiguration.getJumpTapTimeout());
        maxConsecutiveTapNanoTime =
                3 * TimeUnit.MILLISECONDS.toNanos(ViewConfiguration.getDoubleTapTimeout());
    }

    @Override
    public boolean onDown(MotionEvent ev) {
        if (ev.getPointerCount() != 1)
            return false;

        final float x = ev.getX();
        final float y = ev.getY();
        final long nanoTime = System.nanoTime();

        if (consecutiveTapCount == 0 ||
                nanoTime - lastTouchDownNanoTime < maxConsecutiveTapNanoTime &&
                        Math.abs(x - lastTouchDownX) < maxMultiTapSlop &&
                        Math.abs(y - lastTouchDownY) < maxMultiTapSlop) {
            lastTouchDownNanoTime = nanoTime;
            lastTouchDownX = x;
            lastTouchDownY = y;
        } else {
            hidePrompt();
            consecutiveTapCount = 0;
        }
        return true;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent ev) {
        final float x = ev.getX();
        final float y = ev.getY();
        final long nanoTime = System.nanoTime();

        if (Math.abs(x - lastTouchDownX) < maxTapSlop &&
                Math.abs(y - lastTouchDownY) < maxTapSlop &&
                nanoTime - lastTouchDownNanoTime < maxTapNanoTime) {
            ++consecutiveTapCount;

            if (consecutiveTapCount < TRIGGER_TAP_COUNT) {
                displayPrompt(consecutiveTapCount);
            } else {
                consecutiveTapCount = 0;
                if (listener != null) {
                    listener.onMultiTap();
                    hidePrompt();
                }
            }
        }
        return true;
    }

    private void displayPrompt(int tapNumber) {
        if (tapNumber >= 2) {
            String message = context.getResources().getString(
                    R.string.multi_tap_prompt, TRIGGER_TAP_COUNT - tapNumber);
            hidePrompt();
            lastToast = Toast.makeText(context, message, Toast.LENGTH_LONG);
            lastToast.show();
        }
    }

    private void hidePrompt() {
        if (lastToast != null) {
            lastToast.cancel();
            lastToast = null;
        }
    }
}
