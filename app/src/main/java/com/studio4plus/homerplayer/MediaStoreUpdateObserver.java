package com.studio4plus.homerplayer;

import android.database.ContentObserver;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.studio4plus.homerplayer.events.MediaStoreUpdateEvent;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * Observe for changes to media files and post a MediaStoreUpdateEvent to the event bus to trigger
 * a scan for new audiobooks.
 *
 * The onChange method may be called a number of times as media files are being changed on the
 * device. To avoid rescanning often, a rescan is triggered only after RESCAN_DELAY_MS milliseconds
 * have passed since the last onChange call.
 */
public class MediaStoreUpdateObserver extends ContentObserver {

    private static final int RESCAN_DELAY_MS = 5000;

    @NonNull
    private final Handler mainThreadHandler;
    @NonNull
    private final EventBus eventBus;

    @Inject
    public MediaStoreUpdateObserver(@NonNull Handler mainThreadHandler, @NonNull EventBus eventBus) {
        super(mainThreadHandler);
        this.mainThreadHandler = mainThreadHandler;
        this.eventBus = eventBus;
    }

    @Override
    public void onChange(boolean selfChange) {
        mainThreadHandler.removeCallbacks(delayedRescanTask);
        mainThreadHandler.postDelayed(delayedRescanTask, RESCAN_DELAY_MS);
    }

    private final Runnable delayedRescanTask = new Runnable() {
        @Override
        public void run() {
            eventBus.post(new MediaStoreUpdateEvent());
        }
    };
}
