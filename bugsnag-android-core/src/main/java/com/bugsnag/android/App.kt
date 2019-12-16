package com.bugsnag.android

import java.io.IOException

open class App internal constructor(
    config: ImmutableConfig,
    var binaryArch: String?,
    var id: String?,
    var releaseStage: String?,
    var version: String?
) : JsonStream.Streamable {

    var buildUuid: String? = config.buildUuid
    var codeBundleId: String? = config.codeBundleId
    var type: String? = config.appType
    var versionCode: Number? = config.versionCode

    internal open fun serialiseFields(writer: JsonStream) {
        writer.name("binaryArch").value(binaryArch)
        writer.name("buildUUID").value(buildUuid)
        writer.name("codeBundleId").value(codeBundleId)
        writer.name("id").value(id)
        writer.name("releaseStage").value(releaseStage)
        writer.name("type").value(type)
        writer.name("version").value(version)
        writer.name("versionCode").value(versionCode)
    }

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        serialiseFields(writer)
        writer.endObject()
    }
}
