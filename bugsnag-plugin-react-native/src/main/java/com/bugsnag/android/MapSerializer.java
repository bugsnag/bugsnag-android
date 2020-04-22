package com.bugsnag.android;

import java.util.Map;

interface MapSerializer<T> {
    void serialize(Map<String, Object> map, T obj);
}
