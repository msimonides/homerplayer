package com.studio4plus.homerplayer.util;

import com.studio4plus.homerplayer.BuildConfig;

public class VersionUtil {

    private static final String OFFICIAL_VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+$";

    public static boolean isOfficialVersion() {
        return BuildConfig.VERSION_NAME.matches(OFFICIAL_VERSION_PATTERN);
    }
}