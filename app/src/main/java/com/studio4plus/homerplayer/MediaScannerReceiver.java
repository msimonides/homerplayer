package com.studio4plus.homerplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.studio4plus.homerplayer.events.MediaScannerTriggeredEvent;

import de.greenrobot.event.EventBus;

public class MediaScannerReceiver extends BroadcastReceiver {

    private static final MediaScannerTriggeredEvent mediaScannerTriggeredEvent =
            new MediaScannerTriggeredEvent();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("MediaScanner", "path: " + intent.getData());
        EventBus.getDefault().post(mediaScannerTriggeredEvent);
    }
}
