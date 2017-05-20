package com.studio4plus.homerplayer;

import android.annotation.TargetApi;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.studio4plus.homerplayer.events.DeviceAdminChangeEvent;

import de.greenrobot.event.EventBus;

public class HomerPlayerDeviceAdmin extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 21)
            API21.enableLockTask(context);
        EventBus.getDefault().post(new DeviceAdminChangeEvent(true));
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        EventBus.getDefault().post(new DeviceAdminChangeEvent(false));
    }

    public static boolean isDeviceOwner(Context context) {
        return Build.VERSION.SDK_INT >= 21 && API21.isDeviceOwner(context);
    }

    public static void clearDeviceOwner(Context context) {
        if (Build.VERSION.SDK_INT >= 21)
            API21.clearDeviceOwnerAndAdmin(context);
    }

    @TargetApi(21)
    private static class API21 {

        public static boolean isDeviceOwner(Context context) {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            return dpm.isDeviceOwnerApp(context.getPackageName());
        }

        public static void clearDeviceOwnerAndAdmin(Context context) {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            dpm.clearDeviceOwnerApp(context.getPackageName());
            ComponentName adminComponentName = new ComponentName(context, HomerPlayerDeviceAdmin.class);
            dpm.removeActiveAdmin(adminComponentName);
        }

        public static void enableLockTask(Context context) {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminComponentName = new ComponentName(context, HomerPlayerDeviceAdmin.class);
            if (dpm.isAdminActive(adminComponentName) &&
                   dpm.isDeviceOwnerApp(context.getPackageName()))
                dpm.setLockTaskPackages(adminComponentName, new String[]{context.getPackageName()});
        }
    }
}
