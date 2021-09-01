package com.bugsnag.android;

import com.bugsnag.android.internal.DateUtils;

import java.util.Locale;
import java.util.Map;

class BreadcrumbDeserializer implements MapDeserializer<Breadcrumb> {

    private final Logger logger;

    BreadcrumbDeserializer(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Breadcrumb deserialize(Map<String, Object> map) {
        String type = MapUtils.getOrThrow(map, "type");
        String timestamp = MapUtils.getOrThrow(map, "timestamp");

        return new Breadcrumb(
                MapUtils.<String>getOrThrow(map, "message"),
                BreadcrumbType.valueOf(type.toUpperCase(Locale.US)),
                MapUtils.<Map<String, Object>>getOrNull(map, "metadata"),
                DateUtils.fromIso8601(timestamp),
                logger
        );
    }
}
