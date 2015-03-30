package com.studio4plus.homerplayer;

import android.annotation.TargetApi;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class HomerPlayerDeviceAdmin extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        API21.enableLockTask(context);
    }

    @TargetApi(21)
    public static class API21 {
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
