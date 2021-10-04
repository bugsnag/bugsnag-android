package com.bugsnag.android

internal class DeviceSerializer : MapSerializer<DeviceWithState> {
    override fun serialize(map: MutableMap<String, Any?>, device: DeviceWithState) {
        map.putAll(device.toJournalSection())
    }
}
