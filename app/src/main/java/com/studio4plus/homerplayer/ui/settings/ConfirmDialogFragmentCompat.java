package com.studio4plus.homerplayer.ui.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceDialogFragmentCompat;

public class ConfirmDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    static ConfirmDialogFragmentCompat newInstance(@NonNull String key) {
        ConfirmDialogFragmentCompat fragment = new ConfirmDialogFragmentCompat();
        Bundle b = new Bundle(1);
        b.putString("key", key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onDialogClosed(boolean isPositive) {
        ((ConfirmDialogPreference) getPreference()).onDialogClosed(isPositive);
    }
}
