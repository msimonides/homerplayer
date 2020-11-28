package com.studio4plus.homerplayer;

import android.annotation.TargetApi;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import com.studio4plus.homerplayer.events.DeviceAdminChangeEvent;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class HomerPlayerDeviceAdmin extends DeviceAdminReceiver {

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        if (Build.VERSION.SDK_INT >= 21)
            API21.enableLockTask(context);
        EventBus eventBus = HomerPlayerApplication.getComponent(context).getEventBus();
        eventBus.post(new DeviceAdminChangeEvent(true));
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        EventBus eventBus = HomerPlayerApplication.getComponent(context).getEventBus();
        eventBus.post(new DeviceAdminChangeEvent(false));
    }

    public static boolean isDeviceOwner(@NonNull Context context) {
        return Build.VERSION.SDK_INT >= 21 && API21.isDeviceOwner(context);
    }

    public static void clearDeviceOwner(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 21)
            API21.clearDeviceOwnerAndAdmin(context);
    }

    @TargetApi(21)
    private static class API21 {

        public static boolean isDeviceOwner(@NonNull Context context) {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            return dpm.isDeviceOwnerApp(context.getPackageName());
        }

        public static void clearDeviceOwnerAndAdmin(@NonNull Context context) {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            dpm.clearDeviceOwnerApp(context.getPackageName());
            ComponentName adminComponentName = new ComponentName(context, HomerPlayerDeviceAdmin.class);
            dpm.removeActiveAdmin(adminComponentName);
        }

        public static void enableLockTask(@NonNull Context context) {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminComponentName = new ComponentName(context, HomerPlayerDeviceAdmin.class);
            if (dpm.isAdminActive(adminComponentName) &&
                   dpm.isDeviceOwnerApp(context.getPackageName()))
                dpm.setLockTaskPackages(adminComponentName, new String[]{context.getPackageName()});
        }
    }
}
