package com.bugsnag.android;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class SetUtils {
    static <T> Set<T> sanitiseSet(@Nullable Set<T> data) {
        if (data == null) {
            return null;
        }

        Set<T> copy = new HashSet<>(data);

        for (Iterator<T> iterator = copy.iterator(); iterator.hasNext(); ) {
            T obj = iterator.next();
            if (obj == null) {
                iterator.remove();
            }
        }
        return copy;
    }
}
