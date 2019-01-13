package com.studio4plus.homerplayer.concurrency;

import androidx.annotation.NonNull;

/**
 * A very simple future that has listeners for notifying when the result is available.
 */
public interface SimpleFuture<V> {

    interface Listener<V> {
        void onResult(@NonNull V result);
        void onException(@NonNull Throwable t);
    }

    void addListener(@NonNull Listener<V> listener);
    void removeListener(@NonNull Listener<V> listener);
}
