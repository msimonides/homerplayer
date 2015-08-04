package com.studio4plus.homerplayer.events;

import com.studio4plus.homerplayer.battery.BatteryStatus;

public class BatteryStatusChangeEvent {
    public final BatteryStatus batteryStatus;


    public BatteryStatusChangeEvent(BatteryStatus batteryStatus) {
        this.batteryStatus = batteryStatus;
    }
}
