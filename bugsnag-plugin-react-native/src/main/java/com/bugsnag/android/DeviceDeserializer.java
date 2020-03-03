package com.bugsnag.android;

import java.util.Date;
import java.util.Map;

class DeviceDeserializer implements MapDeserializer<DeviceWithState> {

    @Override
    public DeviceWithState deserialize(Map<String, Object> map) {
        Number num = MapUtils.getOrNull(map, "apiLevel");
        Integer apiLevel = num != null ? num.intValue() : null;

        DeviceBuildInfo buildInfo = new DeviceBuildInfo(
                MapUtils.<String>getOrNull(map, "manufacturer"),
                MapUtils.<String>getOrNull(map, "model"),
                MapUtils.<String>getOrNull(map, "osVersion"),
                apiLevel,
                MapUtils.<String>getOrNull(map, "osBuild"),
                MapUtils.<String>getOrNull(map, "fingerprint"),
                MapUtils.<String>getOrNull(map, "tags"),
                MapUtils.<String>getOrNull(map, "brand"),
                MapUtils.<String[]>getOrNull(map, "cpuAbis")
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
                getLong(map, "totalMemory"),
                getLong(map, "freeDisk"),
                getLong(map, "freeMemory"),
                MapUtils.<String>getOrNull(map, "orientation"),
                date
        );
    }

    private Long getLong(Map<String, Object> map, String key) {
        Number num = MapUtils.getOrNull(map, key);
        return num != null ? num.longValue() : null;
    }
}
