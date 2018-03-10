package com.studio4plus.homerplayer;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.events.KioskModeChanged;
import com.studio4plus.homerplayer.ui.HomeActivity;
import com.studio4plus.homerplayer.ui.MainActivity;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

@ApplicationScope
public class KioskModeSwitcher {

    private final Context context;
    private final GlobalSettings globalSettings;
    private final EventBus eventBus;

    @Inject
    KioskModeSwitcher(Context applicationContext, GlobalSettings globalSettings,
                             EventBus eventBus) {
        this.context = applicationContext;
        this.globalSettings = globalSettings;
        this.eventBus = eventBus;
    }

    public boolean isLockTaskPermitted() {
        return Build.VERSION.SDK_INT >= 21 && API21.isLockTaskPermitted(context);
    }

    public void onFullKioskModeEnabled(boolean fullKioskEnabled) {
        Preconditions.checkState(!fullKioskEnabled || isLockTaskPermitted());

        if (fullKioskEnabled)
            API21.setPreferredHomeActivity(context, MainActivity.class);
        else
            API21.clearPreferredHomeActivity(context);

        if (globalSettings.isSimpleKioskModeEnabled())
            onSimpleKioskModeEnabled(!fullKioskEnabled);

        eventBus.post(new KioskModeChanged(KioskModeChanged.Type.FULL, fullKioskEnabled));
    }

    public void onSimpleKioskModeEnabled(boolean enable) {
        if (globalSettings.isFullKioskModeEnabled() & enable)
            return;

        HomeActivity.setEnabled(context, enable);
        if (enable)
            triggerHomeAppSelectionIfNecessary();
        eventBus.post(new KioskModeChanged(KioskModeChanged.Type.SIMPLE, enable));
    }

    private void triggerHomeAppSelectionIfNecessary() {
        final Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.addCategory(Intent.CATEGORY_DEFAULT);

        PackageManager pm = context.getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(homeIntent, 0);
        if (resolveInfo.activityInfo.name.equals("com.android.internal.app.ResolverActivity")) {
            context.startActivity(homeIntent);
        }
    }

    @TargetApi(21)
    static class API21 {

        static boolean isLockTaskPermitted(Context context) {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            return dpm != null && dpm.isLockTaskPermitted(context.getPackageName())
                    && dpm.isDeviceOwnerApp(context.getPackageName());
        }

        static void setPreferredHomeActivity(Context context, Class activityClass) {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            Preconditions.checkNotNull(dpm);
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
            intentFilter.addCategory(Intent.CATEGORY_HOME);
            intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
            ComponentName adminComponentName =
                    new ComponentName(context, HomerPlayerDeviceAdmin.class);
            ComponentName activityComponentName =
                    new ComponentName(context, activityClass);
            dpm.addPersistentPreferredActivity(
                    adminComponentName, intentFilter, activityComponentName);
        }

        static void clearPreferredHomeActivity(Context context) {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            Preconditions.checkNotNull(dpm);
            ComponentName adminComponentName =
                    new ComponentName(context, HomerPlayerDeviceAdmin.class);
            dpm.clearPackagePersistentPreferredActivities(
                    adminComponentName, context.getPackageName());
        }
    }
}
