package com.bugsnag.android;

import java.util.Date;
import java.util.Map;

class DeviceDeserializer implements MapDeserializer<DeviceWithState> {

    @Override
    public DeviceWithState deserialize(Map<String, Object> map) {
        DeviceBuildInfo buildInfo = new DeviceBuildInfo(
                MapUtils.<String>getOrThrow(map, "manufacturer"),
                MapUtils.<String>getOrThrow(map, "model"),
                MapUtils.<String>getOrThrow(map, "osVersion"),
                MapUtils.<Integer>getOrThrow(map, "apiLevel"),
                MapUtils.<String>getOrThrow(map, "osBuild"),
                MapUtils.<String>getOrThrow(map, "fingerprint"),
                MapUtils.<String>getOrThrow(map, "tags"),
                MapUtils.<String>getOrThrow(map, "brand"),
                MapUtils.<String[]>getOrThrow(map, "cpuAbis")
        );

        String time = MapUtils.getOrNull(map, "time");
        Date date = null;

        if (time != null) {
            date = DateUtils.fromIso8601(time);
        }
        return new DeviceWithState(
                buildInfo,
                MapUtils.<Boolean>getOrNull(map, "jailbroken"),
                MapUtils.<String>getOrNull(map, "id"),
                MapUtils.<String>getOrNull(map, "locale"),
                MapUtils.<Long>getOrNull(map, "totalMemory"),
                MapUtils.<Long>getOrNull(map, "freeDisk"),
                MapUtils.<Long>getOrNull(map, "freeMemory"),
                MapUtils.<String>getOrNull(map, "orientation"),
                date
        );
    }
}
