package com.bugsnag.android;

import android.support.annotation.Nullable;

import com.facebook.infer.annotation.ThreadSafe;

import java.util.Map;

@ThreadSafe
final class MapUtils {

    @Nullable
    static String getStringFromMap(String key, Map<String, Object> map) {
        Object packageName = map.get(key);
        return packageName instanceof String ? (String) packageName : null;
    }

}
