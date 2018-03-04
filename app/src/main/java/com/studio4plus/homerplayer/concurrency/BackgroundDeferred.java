package com.studio4plus.homerplayer.concurrency;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class BackgroundDeferred<V> extends BaseDeferred<V> implements Runnable {

    private final @NonNull List<Listener<V>> listeners = new ArrayList<>();

    private final @NonNull Callable<V> task;
    private final @NonNull Handler mainThreadHandler;

    BackgroundDeferred(@NonNull Callable<V> task, @NonNull Handler mainThreadHandler) {
        this.task = task;
        this.mainThreadHandler = mainThreadHandler;
    }

    @Override
    public void run() {
        try {
            final @NonNull V newResult = task.call();
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    setResult(newResult);
                }
            });
        } catch (final Exception e) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    setException(e);
                }
            });
        }
    }
}
