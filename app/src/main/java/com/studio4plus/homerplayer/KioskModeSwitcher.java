package com.studio4plus.homerplayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    public void onFullKioskModeEnabled(@Nullable Activity activity, boolean fullKioskEnabled) {
        Preconditions.checkState(!fullKioskEnabled || isLockTaskPermitted());

        if (globalSettings.isSimpleKioskModeEnabled())
            onSimpleKioskModeEnabled(activity, !fullKioskEnabled);

        if (activity != null) {
            eventBus.post(new KioskModeChanged(activity, KioskModeChanged.Type.FULL, fullKioskEnabled));
        }

        HomeActivity.setEnabled(context, fullKioskEnabled);
        if (fullKioskEnabled)
            API21.setPreferredHomeActivity(context, HomeActivity.class);
        else
            API21.clearPreferredHomeActivity(context);
    }

    public void onSimpleKioskModeEnabled(@Nullable Activity activity, boolean enable) {
        if (globalSettings.isFullKioskModeEnabled() & enable)
            return;

        HomeActivity.setEnabled(context, enable);
        if (enable)
            triggerHomeAppSelectionIfNecessary();
        if (activity != null) {
            eventBus.post(new KioskModeChanged(activity, KioskModeChanged.Type.SIMPLE, enable));
        }
    }

    @NonNull
    public String statusForDiagnosticLog() {
        return API21.statusForDiagnosticLog(context);
    }

    private void triggerHomeAppSelectionIfNecessary() {
        final Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.addCategory(Intent.CATEGORY_DEFAULT);

        // Necessary because application context is used.
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

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

        @NonNull
        static String statusForDiagnosticLog(@NonNull Context context) {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm != null) {
                return "permitted: " + dpm.isLockTaskPermitted(context.getPackageName()) +
                        "; is device owner: " + dpm.isDeviceOwnerApp(context.getPackageName());
            } else {
                return "no DPM!";
            }
        }
    }
}
