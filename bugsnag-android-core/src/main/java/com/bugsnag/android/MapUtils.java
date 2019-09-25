package com.bugsnag.android;

import androidx.annotation.Nullable;

import java.util.Map;

final class MapUtils {

    @Nullable
    static String getStringFromMap(String key, Map<String, Object> map) {
        Object packageName = map.get(key);
        return packageName instanceof String ? (String) packageName : null;
    }

}
