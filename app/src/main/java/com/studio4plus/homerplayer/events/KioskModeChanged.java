package com.studio4plus.homerplayer.events;

import android.app.Activity;

public class KioskModeChanged {
    public enum Type { FULL, SIMPLE }

    public final Activity activity;
    public final Type type;
    public final boolean isEnabled;

    public KioskModeChanged(Activity activity, Type type, boolean isEnabled) {
        this.activity = activity;
        this.type = type;
        this.isEnabled = isEnabled;
    }
}
