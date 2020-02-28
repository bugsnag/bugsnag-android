package com.bugsnag.android;

import java.util.Map;

interface WritableMapSerializer<T> {
    void serialize(Map<String, Object> map, T obj);
}
