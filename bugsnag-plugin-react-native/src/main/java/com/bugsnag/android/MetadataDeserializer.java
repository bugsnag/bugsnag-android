package com.bugsnag.android;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class MetadataDeserializer implements MapDeserializer<Metadata> {
    @Override
    public Metadata deserialize(Map<String, Object> map) {
        // cast map to retain original signature until next major version bump, as this
        // method signature is used by Unity/React native
        @SuppressWarnings({"unchecked", "rawtypes"})
        Map<String, Map<String, Object>> data = (Map) map;
        ConcurrentHashMap<String, Map<String, Object>> store = new ConcurrentHashMap<>(data);
        return new Metadata(store);
    }
}
