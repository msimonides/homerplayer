package com.studio4plus.homerplayer.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.studio4plus.homerplayer.R;

import java.util.concurrent.TimeUnit;

/**
 * A FrameLayout that can recognise a multiple tap (as in double tap, but with more taps).
 * It doesn't intercept any events, merely watches them and passes them to its children.
 *
 * The intended use is for triggering "hidden" functions.
 */
public class MultiTapInterceptor extends FrameLayout {

    public interface Listener {
        void onMultiTap(View view);
    }

    private static final int TRIGGER_TAP_COUNT = 5;

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

    public MultiTapInterceptor(Context context) {
        super(context);
        init(context);
    }

    public MultiTapInterceptor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MultiTapInterceptor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @SuppressWarnings("UnusedDeclaration")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MultiTapInterceptor(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        ViewConfiguration configuration = ViewConfiguration.get(context);
        maxTapSlop = configuration.getScaledTouchSlop();
        maxMultiTapSlop = configuration.getScaledDoubleTapSlop();
        maxTapNanoTime = TimeUnit.MILLISECONDS.toNanos(ViewConfiguration.getJumpTapTimeout());
        maxConsecutiveTapNanoTime =
                3 * TimeUnit.MILLISECONDS.toNanos(ViewConfiguration.getDoubleTapTimeout());
    }

    public void setOnMultitapListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;

        if (action == MotionEvent.ACTION_DOWN && ev.getPointerCount() == 1) {
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

                if (consecutiveTapCount == TRIGGER_TAP_COUNT)
                    return true;
            } else {
                hidePrompt();
                consecutiveTapCount = 0;
            }
        } else if (action == MotionEvent.ACTION_UP && ev.getPointerCount() == 1) {
            final float x = ev.getX();
            final float y = ev.getY();
            final long nanoTime = System.nanoTime();

            if (Math.abs(x - lastTouchDownX) < maxTapSlop &&
                    Math.abs(y - lastTouchDownY) < maxTapSlop &&
                    nanoTime - lastTouchDownNanoTime < maxTapNanoTime) {
                ++consecutiveTapCount;

                if (consecutiveTapCount == TRIGGER_TAP_COUNT)
                    return true;

                displayPrompt(consecutiveTapCount);
            } else {
                hidePrompt();
                consecutiveTapCount = 0;
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;

        switch(action) {
            case MotionEvent.ACTION_DOWN: {
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
            case MotionEvent.ACTION_UP: {
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
                            listener.onMultiTap(this);
                            hidePrompt();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void displayPrompt(int tapNumber) {
        if (tapNumber >= 2) {
            String message = String.format(
                    getResources().getString(R.string.multi_tap_prompt), TRIGGER_TAP_COUNT - tapNumber);
            hidePrompt();
            lastToast = Toast.makeText(getContext(), message, Toast.LENGTH_LONG);
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
