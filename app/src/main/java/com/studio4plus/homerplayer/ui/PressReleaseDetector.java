package com.studio4plus.homerplayer.ui;

import androidx.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;

public class PressReleaseDetector implements View.OnTouchListener {

    public interface Listener {
        void onPressed(View v, float x, float y);
        void onReleased(View v, float x, float y);
    }

    private final Listener listener;
    private boolean isPressed;

    public PressReleaseDetector(@NonNull Listener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            isPressed = true;
            listener.onPressed(v, event.getX(), event.getY());
            return true;
        } else if (isPressed && event.getAction() == MotionEvent.ACTION_UP) {
            listener.onReleased(v, event.getX(), event.getY());
            return true;
        }
        return false;
    }
}
