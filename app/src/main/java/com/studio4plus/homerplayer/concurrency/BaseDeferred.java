package com.studio4plus.homerplayer.concurrency;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A straightforward implementation of SimpleFuture.
 * It's intended for use only on a single thread.
 *
 * Note: I don't need the full power of ListenableFutures nor Rx yet.
 */
public class BaseDeferred<V> implements SimpleFuture<V> {

    private final @NonNull List<Listener<V>> listeners = new ArrayList<>();
    private @Nullable V result;
    private @Nullable Throwable exception;

    @Override
    public void addListener(@NonNull Listener<V> listener) {
        listeners.add(listener);
        if (result != null)
            listener.onResult(result);
        else if (exception != null)
            listener.onException(exception);
    }

    @Override
    public void removeListener(@NonNull Listener<V> listener) {
        listeners.remove(listener);
    }

    protected void setResult(@NonNull V result) {
        this.result = result;
        for (Listener<V> listener : listeners)
            listener.onResult(result);
    }

    protected void setException(@NonNull Throwable exception) {
        this.exception = exception;
        for (Listener<V> listener : listeners)
            listener.onException(exception);
    }
}
