package com.bugsnag.android;

import com.bugsnag.android.internal.DateUtils;

import java.util.Date;
import java.util.Map;

class DeviceDeserializer implements MapDeserializer<DeviceWithState> {

    @Override
    public DeviceWithState deserialize(Map<String, Object> map) {
        DeviceBuildInfo buildInfo = new DeviceBuildInfo(
                MapUtils.<String>getOrNull(map, "manufacturer"),
                MapUtils.<String>getOrNull(map, "model"),
                MapUtils.<String>getOrNull(map, "osVersion"),
                MapUtils.getInt(map, "apiLevel"),
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
                MapUtils.getLong(map, "totalMemory"),
                MapUtils.<Map<String, Object>>getOrNull(map, "runtimeVersions"),
                MapUtils.getLong(map, "freeDisk"),
                MapUtils.getLong(map, "freeMemory"),
                MapUtils.<String>getOrNull(map, "orientation"),
                date
        );
    }
}
