package com.bugsnag.android;

import java.util.Map;

class MapUtils {

    static String getString(Map<String, Object> map, String key) {
        Object id = map.get(key);

        if (id instanceof String) {
            return (String) id;
        } else {
            return null;
        }
    }
}
