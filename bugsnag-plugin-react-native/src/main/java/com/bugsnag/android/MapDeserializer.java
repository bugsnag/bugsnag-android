package com.bugsnag.android;

import java.util.Map;

interface MapDeserializer<T> {
    T deserialize(Map<String, Object> map);
}
