package com.studio4plus.homerplayer.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

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
    private long lastTapNanoTime;
    private float lastTapX;
    private float lastTapY;
    private int consecutiveTapCount;

    private int maxTapSlop;
    private int maxMultiTapSlop;
    private long maxTapNanoTime;
    private long maxDoubleTapNanoTime;

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
        final int MS_TO_NANO_TIME = 1000000;
        ViewConfiguration configuration = ViewConfiguration.get(context);
        maxTapSlop = configuration.getScaledTouchSlop();
        maxMultiTapSlop = configuration.getScaledDoubleTapSlop() * 2;
        maxTapNanoTime = ViewConfiguration.getJumpTapTimeout() * MS_TO_NANO_TIME;
        maxDoubleTapNanoTime = ViewConfiguration.getDoubleTapTimeout() * MS_TO_NANO_TIME;
    }

    public void setOnMultitapListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;

        switch(action) {
            case MotionEvent.ACTION_DOWN:
                if (ev.getPointerCount() == 1) {
                    lastTouchDownNanoTime = System.nanoTime();
                    lastTouchDownX =  ev.getX();
                    lastTouchDownY = ev.getY();

                    if (lastTouchDownNanoTime - lastTapNanoTime > maxDoubleTapNanoTime ||
                            Math.abs(lastTouchDownX - lastTapX) > maxMultiTapSlop ||
                            Math.abs(lastTouchDownY - lastTapY) > maxMultiTapSlop) {
                        consecutiveTapCount = 0;
                    }
                    break;
                }
            case MotionEvent.ACTION_UP:
                if (System.nanoTime() - lastTouchDownNanoTime < maxTapNanoTime) {
                    final float x = ev.getX();
                    final float y = ev.getY();
                    if (Math.abs(lastTouchDownX - x) < maxTapSlop &&
                            Math.abs(lastTouchDownY - y) < maxTapSlop) {

                        lastTapX = x;
                        lastTapY = y;
                        lastTapNanoTime = System.nanoTime();

                        ++consecutiveTapCount;
                        if (consecutiveTapCount == TRIGGER_TAP_COUNT) {
                            consecutiveTapCount = 0;
                            if (listener != null)
                                listener.onMultiTap(this);
                        }
                    } else {
                        consecutiveTapCount = 0;
                    }
                }
                lastTouchDownNanoTime = 0;
                break;
        }
        return false;
    }
}
