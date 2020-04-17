package com.bugsnag.android;

import java.util.Map;

class MapUtils {

    @SuppressWarnings("unchecked")
    static <T> T getOrNull(Map<String, Object> map, String key) {
        Object id = map.get(key);
        return id != null ? (T) id : null;
    }

    @SuppressWarnings("unchecked")
    static <T> T getOrThrow(Map<String, Object> map, String key) {
        Object id = map.get(key);
        if (id != null) {
            return (T) id;
        } else {
            throw new IllegalArgumentException("Missing required parameter " + key);
        }
    }

    static Long getLong(Map<String, Object> map, String key) {
        Number num = MapUtils.getOrNull(map, key);
        return num != null ? num.longValue() : null;
    }

    static Integer getInt(Map<String, Object> map, String key) {
        Number num = MapUtils.getOrNull(map, key);
        return num != null ? num.intValue() : null;
    }
}
