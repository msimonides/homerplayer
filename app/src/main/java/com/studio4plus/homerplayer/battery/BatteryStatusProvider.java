package com.studio4plus.homerplayer.battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.annotation.NonNull;

import com.studio4plus.homerplayer.events.BatteryStatusChangeEvent;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class BatteryStatusProvider extends BroadcastReceiver {

    private final IntentFilter batteryStatusIntentFilter;
    private final Context applicationContext;
    private final EventBus eventBus;

    @Inject
    public BatteryStatusProvider(Context applicationContext, EventBus eventBus) {
        this.applicationContext = applicationContext;
        this.eventBus = eventBus;
        batteryStatusIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    }

    public void start() {
        Intent batteryStatusIntent = applicationContext.registerReceiver(this, batteryStatusIntentFilter);
        if (batteryStatusIntent != null)
            notifyBatteryStatus(getBatteryStatus(batteryStatusIntent));
    }

    public void stop() {
        applicationContext.unregisterReceiver(this);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (batteryStatusIntentFilter.matchAction(intent.getAction())) {
            notifyBatteryStatus(getBatteryStatus(intent));
        }
    }

    private void notifyBatteryStatus(BatteryStatus batteryStatus) {
        eventBus.postSticky(new BatteryStatusChangeEvent(batteryStatus));
    }

    private BatteryStatus getBatteryStatus(@NonNull Intent batteryStatusIntent) {
        int status = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        ChargeLevel chargeLevel;
        if (status == BatteryManager.BATTERY_STATUS_FULL) {
            chargeLevel = ChargeLevel.FULL;
        } else {
            int level = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float chargePercent = level / (float) scale;
            chargeLevel = fromPercentage(chargePercent);
        }

        return new BatteryStatus(chargeLevel, isCharging);
    }

    private static ChargeLevel fromPercentage(float percentage) {
        if (percentage < 0.25f)
            return ChargeLevel.CRITICAL;
        else if (percentage < 0.5f)
            return ChargeLevel.LEVEL_1;
        else if (percentage < 0.75f)
            return ChargeLevel.LEVEL_2;
        else if (percentage < 1.0f)
            return ChargeLevel.LEVEL_3;
        else
            return ChargeLevel.FULL;
    }
}
