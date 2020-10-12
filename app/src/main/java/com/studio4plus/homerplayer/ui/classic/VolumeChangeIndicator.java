package com.studio4plus.homerplayer.ui.classic;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.studio4plus.homerplayer.R;

public class VolumeChangeIndicator extends View {

    private int minValue;
    private int maxValue;
    private int currentValue;

    @NonNull
    private final Path path = new Path();
    @NonNull
    private final Paint indicatorPaint = new Paint();

    private int indicatorColor = 0xffffffff;

    public VolumeChangeIndicator(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    public VolumeChangeIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, R.attr.volumeChangeIndicatorStyle, 0);
    }

    public VolumeChangeIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(21)
    public VolumeChangeIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs, R.styleable.VolumeChangeIndicator, defStyleAttr, defStyleRes);
            try {
                indicatorColor = a.getColor(
                        R.styleable.VolumeChangeIndicator_android_color, 0xffffffff);
            } finally {
                a.recycle();
            }
        }
        indicatorPaint.setColor(indicatorColor);
    }

    public void setVolume(int min, int max, int volume) {
        minValue = min;
        maxValue = max;
        currentValue = volume;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int totalSteps = maxValue - minValue;
        final int volumeSteps = currentValue - minValue;
        final int width = getWidth() - getPaddingLeft() - getPaddingStart();
        final int height = getHeight() - getPaddingTop() - getPaddingBottom();

        // Add 1 to display a single volume step at min volume.
        final float indicatorFraction = ((float) volumeSteps + 1f) / (totalSteps + 1);
        final int indicatorWidth = Math.round(indicatorFraction * width);
        final int indicatorHeight = Math.round(indicatorFraction * height);

        final int lowEndX;
        final int highEndX;
        if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
            lowEndX = width + getPaddingRight();
            highEndX = lowEndX - indicatorWidth;
        } else {
            lowEndX = getPaddingLeft();
            highEndX = lowEndX + indicatorWidth;
        }
        final int lowEndY = getPaddingTop() + height;
        final int highEndY = lowEndY - indicatorHeight;

        path.reset();
        path.moveTo(lowEndX, lowEndY);
        path.lineTo(highEndX, lowEndY);
        path.lineTo(highEndX, highEndY);
        path.close();
        canvas.drawPath(path, indicatorPaint);
    }
}
