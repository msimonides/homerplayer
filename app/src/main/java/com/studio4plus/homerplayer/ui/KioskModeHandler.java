package com.studio4plus.homerplayer.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;

import com.studio4plus.homerplayer.GlobalSettings;
import com.studio4plus.homerplayer.events.KioskModeChanged;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;

@Singleton
public class KioskModeHandler {

    private final GlobalSettings globalSettings;
    private boolean keepNavigation = false;

    private boolean isLockEnabled = false;

    @Inject
    public KioskModeHandler(GlobalSettings settings, EventBus eventBus) {
        this.globalSettings = settings;
        eventBus.register(this);
    }

    public void setKeepNavigation(Boolean keepNavigation) {
        this.keepNavigation = keepNavigation;
    }

    public void onActivityStart(@NonNull Activity activity) {
        setUiFlagsAndLockTask(activity);
    }

    public void onFocusGained(@NonNull Activity activity) {
        setUiFlagsAndLockTask(activity);
    }

    @SuppressWarnings("unused")
    public void onEvent(KioskModeChanged event) {
        if (event.type == KioskModeChanged.Type.FULL)
            lockTask(event.activity, event.isEnabled);
        setNavigationVisibility(event.activity, !event.isEnabled);
    }

    private void setUiFlagsAndLockTask(@NonNull Activity activity) {
        int visibilitySetting = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (!keepNavigation) {
            visibilitySetting |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        }

        activity.getWindow().getDecorView().setSystemUiVisibility(visibilitySetting);
        if (globalSettings.isAnyKioskModeEnabled())
            setNavigationVisibility(activity, false);

        if (globalSettings.isFullKioskModeEnabled())
            lockTask(activity, true);
    }

    private void setNavigationVisibility(@NonNull Activity activity, boolean show) {
        if (Build.VERSION.SDK_INT < 19 || keepNavigation)
            return;

        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        View decorView = activity.getWindow().getDecorView();
        int visibilitySetting = decorView.getSystemUiVisibility();
        if (show)
            visibilitySetting &= ~flags;
        else
            visibilitySetting |= flags;

        decorView.setSystemUiVisibility(visibilitySetting);
    }

    private void lockTask(@NonNull Activity activity, boolean isLocked) {
        if (isLockEnabled != isLocked) {
            isLockEnabled = isLocked;
            if (isLocked)
                API21.startLockTask(activity);
            else
                API21.stopLockTask(activity);
        }
    }

    @TargetApi(21)
    private static class API21 {
        static void startLockTask(Activity activity) {
            activity.startLockTask();
        }

        static void stopLockTask(Activity activity) {
            activity.stopLockTask();
        }
    }
}
