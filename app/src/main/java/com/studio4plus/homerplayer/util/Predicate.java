package com.studio4plus.homerplayer.util;

import androidx.annotation.NonNull;

public interface Predicate<T> {
    boolean isTrue(@NonNull T argument);
}
