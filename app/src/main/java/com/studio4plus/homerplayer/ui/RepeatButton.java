package com.studio4plus.homerplayer.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

public class RepeatButton extends AppCompatButton {

    /**
     * A listener that is called when the view is pressed for a short time but still held pressed.
     * Useful for showing feedback to the user. A click or long press is likely to follow.
     */
    public interface onPressListener {
        void onPressed(@NonNull View view);
    }

    @Nullable
    private onPressListener onPressListener;
    @Nullable
    private Runnable longPressAndRepeat;
    @Nullable
    private Runnable onPressedTrigger;
    private int touchSlop = -1;
    private boolean hasPerformedLongPress = false;

    public RepeatButton(@NonNull Context context) {
        super(context);
    }

    public RepeatButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RepeatButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnPressListener(@Nullable onPressListener onPressListener) {
        this.onPressListener = onPressListener;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();
        final int action = event.getActionMasked();

        if (touchSlop < 0) {
            touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        }

        switch(action) {
            case MotionEvent.ACTION_DOWN:
                hasPerformedLongPress = false;
                // View.onTouchEvent has an additional delay in scrolling containers, this class
                // doesn't bother with that.
                setPressed(x, y);
                postLongPress(x, y);
                postOnPressedTrigger();
                break;
            case MotionEvent.ACTION_UP:
                abortCallbacks();
                if (!hasPerformedLongPress) {
                    boolean focusTaken = false;
                    if (isFocusable() && isFocusableInTouchMode() && !isFocused()) {
                        focusTaken = requestFocus();
                    }

                    if (!focusTaken) {
                        if (!post(this::performClick)) {
                            performClick();
                        }
                    }
                }

                Runnable unpress = () -> setPressed(false);
                if (!post(unpress)) {
                    unpress.run();
                }
                hasPerformedLongPress = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (Build.VERSION.SDK_INT >= 21) {
                    drawableHotspotChanged(x, y);
                }

                if (!pointInView(x, y, touchSlop)) {
                    abortCallbacks();
                    setPressed(false);
                    hasPerformedLongPress = false;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                abortCallbacks();
                setPressed(false);
                hasPerformedLongPress = false;
                break;
        }
        return true;
    }

    private void setPressed(float x, float y) {
        if (Build.VERSION.SDK_INT >= 21) {
            drawableHotspotChanged(x, y);
        }
        setPressed(true);
    }

    private boolean performLongClickCompat(float x, float y) {
        if (Build.VERSION.SDK_INT >= 24) {
            return performLongClick(x, y);
        } else {
            return performLongClick();
        }
    }

    private void postOnPressedTrigger() {
        if (onPressedTrigger == null) {
            onPressedTrigger = this::triggerOnPressed;
        } else {
            removeCallbacks(onPressedTrigger);
        }
        postDelayed(onPressedTrigger, ViewConfiguration.getTapTimeout());
    }

    private void triggerOnPressed() {
        if (onPressListener != null) {
            onPressListener.onPressed(this);
        }
    }

    private void postLongPress(float x, float y) {
        if (longPressAndRepeat == null) {
            longPressAndRepeat = new LongPressAndRepeat(getWindowAttachCount(), x, y);
        } else {
            removeCallbacks(longPressAndRepeat);
        }
        postDelayed(longPressAndRepeat, ViewConfiguration.getLongPressTimeout());
    }

    public boolean pointInView(float localX, float localY, float slop) {
        return localX >= -slop && localY >= -slop && localX < (getWidth() + slop) &&
                localY < (getHeight() + slop);
    }

    private void abortCallbacks() {
        if (longPressAndRepeat != null) {
            removeCallbacks(longPressAndRepeat);
            longPressAndRepeat = null;
        }
        if (onPressedTrigger != null) {
            removeCallbacks(onPressedTrigger);
            onPressedTrigger = null;
        }
    }

    private class LongPressAndRepeat implements Runnable {
        private int originalWindowAttachCount;
        private float x;
        private float y;

        private LongPressAndRepeat(int originalWindowAttachCount, float x, float y) {
            this.originalWindowAttachCount = originalWindowAttachCount;
            this.x = x;
            this.y = y;
        }

        @Override
        public void run() {
            if ((isPressed()) && (getParent() != null)
                    && originalWindowAttachCount == getWindowAttachCount()) {
                if (performLongClickCompat(x, y)) {
                    hasPerformedLongPress = true;
                }
                if (longPressAndRepeat != null) {
                    postDelayed(longPressAndRepeat, ViewConfiguration.getLongPressTimeout());
                }
            }
        }
    }
}
