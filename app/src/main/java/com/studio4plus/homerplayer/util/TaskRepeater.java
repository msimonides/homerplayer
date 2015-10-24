package com.studio4plus.homerplayer.util;

import android.os.Handler;
import android.os.Looper;

import com.google.common.base.Preconditions;

/**
 * Calls a task periodically.
 */
public class TaskRepeater {

    private final Runnable repeatingTask;
    private final Handler handler;
    private boolean isRunning;

    public TaskRepeater(final Runnable task, final Handler handler, final long delay) {
        this.handler = handler;
        this.repeatingTask = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    task.run();
                    handler.postDelayed(this, delay);
                }
            }
        };
    }

    public void start() {
        Preconditions.checkState(!isRunning);
        isRunning = true;
        handler.post(repeatingTask);
    }

    public void stop() {
        Preconditions.checkState(isRunning);
        isRunning = false;
        handler.removeCallbacks(repeatingTask);
    }
}
