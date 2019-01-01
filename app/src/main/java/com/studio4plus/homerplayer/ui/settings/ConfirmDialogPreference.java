package com.studio4plus.homerplayer.ui.settings;

import android.annotation.TargetApi;
import android.content.Context;

import android.support.annotation.Nullable;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import com.studio4plus.homerplayer.R;

class ConfirmDialogPreference extends DialogPreference {

    public interface OnConfirmListener {
        void onConfirmed();
    }

    @Nullable
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

    public ConfirmDialogPreference(Context context) {
        super(context);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.preference_dialog_confirm;
    }

    void setOnConfirmListener(OnConfirmListener listener) {
        this.listener = listener;
    }

    void onDialogClosed(boolean positive) {
        if (positive && listener != null)
            listener.onConfirmed();
    }
}
