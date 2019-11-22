package com.bugsnag.android

import java.io.IOException
import java.util.Date

class Breadcrumb internal constructor(
    val message: String,
    val type: BreadcrumbType,
    val metadata: MutableMap<String, Any?>,
    val timestamp: Date = Date()
) : JsonStream.Streamable {

    internal constructor(message: String) : this(
        "manual",
        BreadcrumbType.MANUAL,
        mutableMapOf(Pair("message", message)),
        Date()
    )

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("timestamp").value(DateUtils.toIso8601(timestamp))
        writer.name("name").value(message)
        writer.name("type").value(type.toString())
        writer.name("metaData")
        writer.value(metadata, true)
        writer.endObject()
    }
}
