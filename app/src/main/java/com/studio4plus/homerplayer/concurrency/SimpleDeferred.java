package com.studio4plus.homerplayer.concurrency;

import androidx.annotation.NonNull;

public class SimpleDeferred<V> extends BaseDeferred<V> {

    public void setResult(@NonNull V result) {
        super.setResult(result);
    }

    public void setException(@NonNull Throwable exception) {
        super.setException(exception);
    }
}