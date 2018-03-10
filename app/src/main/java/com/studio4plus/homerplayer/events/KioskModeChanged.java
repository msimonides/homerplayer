package com.studio4plus.homerplayer.events;

public class KioskModeChanged {
    public enum Type { FULL, SIMPLE }

    public final Type type;
    public final boolean isEnabled;

    public KioskModeChanged(Type type, boolean isEnabled) {
        this.type = type;
        this.isEnabled = isEnabled;
    }
}
