package com.studio4plus.homerplayer.util;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public abstract class LifecycleAwareRunnable implements DefaultLifecycleObserver, Runnable {

    @NonNull
    protected final Handler handler;

    public LifecycleAwareRunnable(@NonNull Handler handler) {
        this.handler = handler;
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        handler.removeCallbacks(this);
    }
}
