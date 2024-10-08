package com.bugsnag.android;

import com.bugsnag.android.internal.dag.ValueProvider;

import java.util.Map;

class AppDeserializer implements MapDeserializer<AppWithState> {

    @Override
    public AppWithState deserialize(Map<String, Object> map) {
        return new AppWithState(
                MapUtils.<String>getOrNull(map, "binaryArch"),
                MapUtils.<String>getOrNull(map, "id"),
                MapUtils.<String>getOrNull(map, "releaseStage"),
                MapUtils.<String>getOrNull(map, "version"),
                MapUtils.<String>getOrNull(map, "codeBundleId"),
                new ValueProvider<>(MapUtils.getOrNull(map, "buildUuid")),
                MapUtils.<String>getOrNull(map, "type"),
                MapUtils.<Number>getOrNull(map, "versionCode"),
                MapUtils.<Number>getOrNull(map, "duration"),
                MapUtils.<Number>getOrNull(map, "durationInForeground"),
                MapUtils.<Boolean>getOrNull(map, "inForeground"),
                MapUtils.<Boolean>getOrNull(map, "isLaunching")
        );
    }
}
