package com.studio4plus.homerplayer.ui;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * A convenience class for binding a MultitapGestureDetectorListener to a View with
 * setOnTouchListener.
 */
public class MultitapTouchListener implements View.OnTouchListener {

    private final GestureDetector gestureDetector;

    public MultitapTouchListener(
            Context context, MultitapGestureDetectorListener.Listener listener) {
        MultitapGestureDetectorListener multitapGestureListener =
                new MultitapGestureDetectorListener(context, listener);
        gestureDetector = new GestureDetector(context, multitapGestureListener);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }
}
