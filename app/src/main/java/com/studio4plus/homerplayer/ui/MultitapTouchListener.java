package com.studio4plus.homerplayer.ui;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Toast;

import com.studio4plus.homerplayer.R;

import java.util.concurrent.TimeUnit;

public class MultitapTouchListener implements View.OnTouchListener {

    private static final int TRIGGER_TAP_COUNT = 5;

    public interface Listener {
        void onMultiTap();
    }

    private final Listener listener;
    private final Context context;

    private long lastTouchDownNanoTime;
    private float initialTouchDownX;
    private float initialTouchDownY;
    private float lastTouchDownX;
    private float lastTouchDownY;
    private int consecutiveTapCount;

    private final int maxTapSlop;
    private final int maxMultiTapSlop;
    private final long maxTapNanoTime;
    private final long maxConsecutiveTapNanoTime;

    private Toast lastToast;

    public MultitapTouchListener(Context context, Listener listener) {
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
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getPointerCount() != 1)
            return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return onTouchDown(event);
            case MotionEvent.ACTION_UP:
                return onTouchUp(event);
            default:
                return false;
        }
    }

    private boolean onTouchDown(MotionEvent ev) {
        final float x = ev.getX();
        final float y = ev.getY();
        final long nanoTime = System.nanoTime();

        if (consecutiveTapCount == 0 ||
                nanoTime - lastTouchDownNanoTime > maxConsecutiveTapNanoTime ||
                Math.abs(x - initialTouchDownX) > maxMultiTapSlop ||
                Math.abs(y - initialTouchDownY) > maxMultiTapSlop) {
            hidePrompt();
            consecutiveTapCount = 0;
            initialTouchDownX = x;
            initialTouchDownY = y;
        }

        lastTouchDownX = x;
        lastTouchDownY = y;
        lastTouchDownNanoTime = nanoTime;
        return true;
    }

    private boolean onTouchUp(MotionEvent ev) {
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
            return true;
        }
        return false;
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
