package com.bugsnag.android;

import java.util.Map;

class MetadataDeserializer implements MapDeserializer<Metadata> {
    @Override
    public Metadata deserialize(Map<String, Object> map) {
        return new Metadata(map);
    }
}
