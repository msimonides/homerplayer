package com.studio4plus.homerplayer;

import androidx.annotation.NonNull;

import javax.inject.Inject;

import timber.log.Timber;

// Disable kiosk modes if crash loop is detected.
public class CrashLoopProtection {

    private final static long QUICK_RESTART_THRESHOLD_MS = 5_000;
    private final static int QUICK_RESTART_COUNT_DISABLE_KIOSK = 5;

    @NonNull
    private final GlobalSettings globalSettings;
    @NonNull
    private final KioskModeSwitcher kioskModeSwitcher;

    @Inject
    public CrashLoopProtection(
            @NonNull GlobalSettings globalSettings, @NonNull KioskModeSwitcher kioskModeSwitcher) {
        this.globalSettings = globalSettings;
        this.kioskModeSwitcher = kioskModeSwitcher;
    }

    public void onAppStart() {
        long now = System.currentTimeMillis();
        long timeSinceLastStart = now - globalSettings.lastStartTimestamp();

        if (timeSinceLastStart < QUICK_RESTART_THRESHOLD_MS) {
            int quick_restart_count = globalSettings.quickConsecutiveStartCount();
            globalSettings.setStartTimeInfo(now, quick_restart_count + 1);
            if (quick_restart_count > QUICK_RESTART_COUNT_DISABLE_KIOSK
                    && globalSettings.isAnyKioskModeEnabled()) {
                Timber.e("Crash loop detected, disabling kiosk mode.");
                globalSettings.setFullKioskModeEnabledNow(false);
                globalSettings.setSimpleKioskModeEnabledNow(false);
                kioskModeSwitcher.onFullKioskModeEnabled(null, false);
                kioskModeSwitcher.onSimpleKioskModeEnabled(null, false);
            }
        } else {
            globalSettings.setStartTimeInfo(now, 0);
        }
    }
}
