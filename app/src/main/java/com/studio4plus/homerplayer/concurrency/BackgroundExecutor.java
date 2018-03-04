package com.studio4plus.homerplayer.concurrency;

import android.os.Handler;
import android.support.annotation.NonNull;

import java.util.concurrent.Callable;

public class BackgroundExecutor {

    private final @NonNull Handler mainThreadHandler;
    private final @NonNull Handler taskHandler;

    public BackgroundExecutor(@NonNull Handler mainThreadHandler, @NonNull Handler taskHandler) {
        this.mainThreadHandler = mainThreadHandler;
        this.taskHandler = taskHandler;
    }

    public <V> SimpleFuture<V> postTask(@NonNull Callable<V> task) {
        BackgroundDeferred<V> deferred = new BackgroundDeferred<>(task, mainThreadHandler);
        taskHandler.post(deferred);
        return deferred;
    }
}
