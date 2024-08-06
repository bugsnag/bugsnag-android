package com.bugsnag.android;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class ErrorDeserializer implements MapDeserializer<Error> {

    private final StackframeDeserializer stackframeDeserializer;
    private final Logger logger;

    ErrorDeserializer(StackframeDeserializer stackframeDeserializer, Logger logger) {
        this.stackframeDeserializer = stackframeDeserializer;
        this.logger = logger;
    }

    @Override
    public Error deserialize(Map<String, Object> map) {
        String type = MapUtils.getOrThrow(map, "type");
        List<Map<String, Object>> stacktrace = MapUtils.getOrThrow(map, "stacktrace");
        List<Stackframe> frames = new ArrayList<>(stacktrace.size());

        for (Map<String, Object> frame : stacktrace) {
            frames.add(stackframeDeserializer.deserialize(frame));
        }

        ErrorInternal impl = new ErrorInternal(
                MapUtils.<String>getOrThrow(map, "errorClass"),
                MapUtils.<String>getOrNull(map, "errorMessage"),
                new Stacktrace(frames),
                ErrorType.valueOf(type.toUpperCase(Locale.US))
        );
        return new Error(impl, logger);
    }
}
