package com.bugsnag.android;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class MetadataDeserializer implements MapDeserializer<Metadata> {
    @Override
    public Metadata deserialize(Map<String, Object> map) {
        ConcurrentHashMap<String, Object> store = new ConcurrentHashMap<>(map);
        return new Metadata(store);
    }
}
