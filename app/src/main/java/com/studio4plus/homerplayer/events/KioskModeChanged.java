package com.studio4plus.homerplayer.events;

import android.app.Activity;

import androidx.annotation.NonNull;

public class KioskModeChanged {
    public enum Type { FULL, SIMPLE }

    public final Activity activity; // TODO: remove Activity from this class.
    public final Type type;
    public final boolean isEnabled;

    public KioskModeChanged(@NonNull Activity activity, Type type, boolean isEnabled) {
        this.activity = activity;
        this.type = type;
        this.isEnabled = isEnabled;
    }
}
