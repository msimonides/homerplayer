package com.studio4plus.homerplayer.util;

import android.os.Looper;

public class DebugUtil {

    public static void verifyIsOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("This code must run on the main thread.");
    }
}
