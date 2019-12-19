package com.bugsnag.android

import java.util.Date

class DeviceWithState internal constructor(
    buildInfo: DeviceBuildInfo,
    cpuAbi: Array<String>,
    jailbroken: Boolean?,
    id: String?,
    locale: String?,
    totalMemory: Long?,
    var freeDisk: Long?,
    var freeMemory: Long?,
    var orientation: String?,
    var time: Date?
) : Device(buildInfo, cpuAbi, jailbroken, id, locale, totalMemory) {

    override fun serializeFields(writer: JsonStream) {
        super.serializeFields(writer)
        writer.name("freeDisk").value(freeDisk)
        writer.name("freeMemory").value(freeMemory)
        writer.name("orientation").value(orientation)

        if (time != null) {
            writer.name("time").value(DateUtils.toIso8601(time!!))
        }
    }
}
