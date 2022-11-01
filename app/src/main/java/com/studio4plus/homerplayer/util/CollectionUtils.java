package com.studio4plus.homerplayer.util;

import androidx.annotation.NonNull;

import com.google.common.base.Function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectionUtils {

    @NonNull
    public static <T, R> List<R> map(@NonNull Collection<T> collection, @NonNull Function<T, R> mapper) {
        List<R> result = new ArrayList<>(collection.size());
        for (T item : collection) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    public static <Type> boolean containsByValue(List<Type> items, Type needle) {
        for (Type item : items)
            if (item.equals(needle))
                return true;
        return false;
    }

    public static <Type> boolean any(@NonNull Collection<Type> collection, @NonNull Predicate<Type> predicate) {
        for (Type item : collection) {
            if (predicate.isTrue(item))
                return true;
        }
        return false;
    }

    public static <Type> List<Type> filter(@NonNull Collection<Type> collection, @NonNull Predicate<Type> predicate) {
        List<Type> result = new ArrayList<>(collection.size());
        for (Type item : collection) {
            if (predicate.isTrue(item)) {
                result.add(item);
            }
        }
        return result;
    }
}
