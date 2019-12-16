package com.bugsnag.android

open class Device internal constructor(
    buildInfo: DeviceBuildInfo,
    var cpuAbi: Array<String>,
    var jailbroken: Boolean?,
    var id: String?,
    var locale: String?,
    var totalMemory: Long?
) : JsonStream.Streamable {

    var manufacturer: String? = buildInfo.manufacturer
    var model: String? = buildInfo.model
    var osName: String? = "android"
    var osVersion: String? = buildInfo.osVersion
    var runtimeVersions: MutableMap<String, Any> = mutableMapOf(
        Pair("androidApiLevel", buildInfo.apiLevel),
        Pair("osBuild", buildInfo.osBuild)
    )


    internal open fun serializeFields(writer: JsonStream) {
        writer.name("cpuAbi").value(cpuAbi)
        writer.name("jailbroken").value(jailbroken)
        writer.name("id").value(id)
        writer.name("locale").value(locale)
        writer.name("manufacturer").value(manufacturer)
        writer.name("model").value(model)
        writer.name("osName").value(osName)
        writer.name("osVersion").value(osVersion)
        writer.name("runtimeVersions").value(runtimeVersions)
        writer.name("totalMemory").value(totalMemory)
    }

    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        serializeFields(writer)
        writer.endObject()
    }
}
