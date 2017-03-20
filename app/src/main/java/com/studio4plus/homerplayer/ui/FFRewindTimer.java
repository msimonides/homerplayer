package com.studio4plus.homerplayer.ui;

import android.os.Handler;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

class FFRewindTimer implements Runnable {

    public interface Observer {
        void onTimerUpdated(long displayTimeMs);
        void onTimerLimitReached();
    }

    private static final int MIN_TICK_INTERVAL_MS = 50;

    private final Handler handler;
    private final List<Observer> observers = new ArrayList<>();
    private final long maxTimeMs;
    private long lastTickAt;
    private long displayTimeMs;
    private int speedMsPerS = 1000;

    FFRewindTimer(Handler handler, long baseDisplayTimeMs, long maxTimeMs) {
        this.handler = handler;
        this.maxTimeMs = maxTimeMs;
        this.displayTimeMs = baseDisplayTimeMs;
        this.lastTickAt = SystemClock.uptimeMillis();
    }

    public void addObserver(Observer observer) {
        this.observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        this.observers.remove(observer);
    }

    public long getDisplayTimeMs() {
        return displayTimeMs;
    }

    public void changeSpeed(int speedMsPerS) {
        stop();
        this.lastTickAt = SystemClock.uptimeMillis();
        this.speedMsPerS = speedMsPerS;
        run();
    }

    @Override
    public void run() {
        long now = SystemClock.uptimeMillis();
        boolean keepRunning = update(now);

        if (keepRunning) {
            long nextTickAt = lastTickAt + Math.max(Math.abs(speedMsPerS), MIN_TICK_INTERVAL_MS);
            handler.postAtTime(this, nextTickAt);
            lastTickAt = now;
        }
    }

    public void stop() {
        handler.removeCallbacks(this);
    }

    private boolean update(long now) {
        long elapsedMs = now - lastTickAt;
        displayTimeMs += (1000 * elapsedMs) / speedMsPerS;

        boolean limitReached = false;
        if (displayTimeMs < 0) {
            displayTimeMs = 0;
            limitReached = true;
        } else if (displayTimeMs > maxTimeMs) {
            displayTimeMs = maxTimeMs;
            limitReached = true;
        }

        int count = observers.size();
        for (int i = 0; i < count; ++i) {
            Observer observer = observers.get(i);
            observer.onTimerUpdated(displayTimeMs);
            if (limitReached)
                observer.onTimerLimitReached();
        }

        return !limitReached;
    }
}
