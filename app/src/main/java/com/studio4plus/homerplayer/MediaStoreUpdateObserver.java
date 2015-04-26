package com.studio4plus.homerplayer;

import android.database.ContentObserver;
import android.os.Handler;

import com.studio4plus.homerplayer.events.MediaStoreUpdateEvent;

import de.greenrobot.event.EventBus;

public class MediaStoreUpdateObserver extends ContentObserver {

    private static final int RESCAN_DELAY_MS = 15000;

    private final Handler mainThreadHandler;

    public MediaStoreUpdateObserver(Handler mainThreadHandler) {
        super(mainThreadHandler);
        this.mainThreadHandler = mainThreadHandler;
    }

    @Override
    public void onChange(boolean selfChange) {
        mainThreadHandler.removeCallbacks(delayedRescanTask);
        mainThreadHandler.postDelayed(delayedRescanTask, RESCAN_DELAY_MS);
    }

    private final Runnable delayedRescanTask = new Runnable() {
        @Override
        public void run() {
            EventBus.getDefault().post(new MediaStoreUpdateEvent());
        }
    };
}
