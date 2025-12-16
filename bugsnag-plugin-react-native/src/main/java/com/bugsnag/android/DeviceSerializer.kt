package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils

internal class DeviceSerializer : MapSerializer<DeviceWithState> {
    override fun serialize(map: MutableMap<String, Any?>, device: DeviceWithState) {
        map["cpuAbi"] = device.cpuAbi?.toList()
        map["jailbroken"] = device.jailbroken
        map["id"] = device.id
        map["locale"] = device.locale
        map["manufacturer"] = device.manufacturer
        map["model"] = device.model
        map["osName"] = device.osName
        map["osVersion"] = device.osVersion
        map["totalMemory"] = device.totalMemory
        map["freeDisk"] = device.freeDisk
        map["freeMemory"] = device.freeMemory
        map["orientation"] = device.orientation

        if (device.time != null) {
            map["time"] = DateUtils.toIso8601(device.time!!)
        }
        map["runtimeVersions"] = device.runtimeVersions
    }
}
