package com.studio4plus.homerplayer.ui;

import androidx.annotation.StyleRes;

import com.studio4plus.homerplayer.R;

public enum ColorTheme {
    CLASSIC_REGULAR(R.style.AppThemeClassic),
    CLASSIC_HIGH_CONTRAST(R.style.AppThemeHighContrast);

    @StyleRes
    public final int styleId;

    ColorTheme(@StyleRes int styleId) {
        this.styleId = styleId;
    }
}
