package com.studio4plus.homerplayer.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class HomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivity(new Intent(this, MainActivity.class));
    }

    public static void setEnabled(Context context, boolean isEnabled) {
        ComponentName componentName = new ComponentName(context, HomeActivity.class);
        int enabledState = isEnabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        context.getPackageManager().setComponentEnabledSetting(
                componentName, enabledState, PackageManager.DONT_KILL_APP);
    }
}
