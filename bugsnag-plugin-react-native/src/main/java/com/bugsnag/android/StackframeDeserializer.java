package com.bugsnag.android;

import java.util.Map;

class StackframeDeserializer implements MapDeserializer<Stackframe> {

    @Override
    public Stackframe deserialize(Map<String, Object> map) {
        return new Stackframe(
                MapUtils.<String>getOrNull(map, "method"),
                MapUtils.<String>getOrNull(map, "file"),
                MapUtils.<Integer>getOrNull(map, "lineNumber"),
                MapUtils.<Boolean>getOrNull(map, "inProject"),
                MapUtils.<Map<String, String>>getOrNull(map, "code"),
                MapUtils.<Integer>getOrNull(map, "columnNumber")
        );
    }
}
