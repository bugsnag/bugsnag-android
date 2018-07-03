package com.bugsnag.android;

import android.support.annotation.Nullable;

import java.util.Map;

final class MapUtils {

    @Nullable
    static String getStringFromMap(String key, Map<String, Object> map) {
        Object packageName = map.get(key);
        return packageName instanceof String ? (String) packageName : null;
    }

    static void putSafely(String key, @Nullable Object nullableObj, Map<String, Object> map) {
        if (nullableObj != null) {
            map.put(key, nullableObj);
        }
    }

}
