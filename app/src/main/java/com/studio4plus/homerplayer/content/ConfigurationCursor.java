package com.studio4plus.homerplayer.content;

import android.database.AbstractCursor;

public class ConfigurationCursor extends AbstractCursor {

    private final static String[] COLUMN_NAMES = { "KioskModeAvailable", "KioskModeEnabled" };
    private final boolean[] values;

    ConfigurationCursor(boolean isKioskModeAvailable, boolean isKioskModeEnabled) {
        this.values = new boolean[]{ isKioskModeAvailable, isKioskModeEnabled };
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public String[] getColumnNames() {
        return COLUMN_NAMES;
    }

    @Override
    public String getString(int i) {
        return Boolean.toString(values[i]);
    }

    @Override
    public short getShort(int i) {
        return (short) (values[i] ? 1 : 0);
    }

    @Override
    public int getInt(int i) {
        return values[i] ? 1 : 0;
    }

    @Override
    public long getLong(int i) {
        return values[i] ? 1 : 0;
    }

    @Override
    public float getFloat(int i) {
        return values[i] ? 1 : 0;
    }

    @Override
    public double getDouble(int i) {
        return values[i] ? 1 : 0;
    }

    @Override
    public boolean isNull(int i) {
        return false;
    }
}
