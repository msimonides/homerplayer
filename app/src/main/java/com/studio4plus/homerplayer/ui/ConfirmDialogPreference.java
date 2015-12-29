package com.studio4plus.homerplayer.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class ConfirmDialogPreference extends DialogPreference {

    public interface OnConfirmListener {
        void onConfirmed();
    }

    private OnConfirmListener listener;

    @TargetApi(21)
    public ConfirmDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ConfirmDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ConfirmDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(21)
    public ConfirmDialogPreference(Context context) {
        super(context);
    }

    public void setOnConfirmListener(OnConfirmListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && listener != null)
            listener.onConfirmed();
    }
}
