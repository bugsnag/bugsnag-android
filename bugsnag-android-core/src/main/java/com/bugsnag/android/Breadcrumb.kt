package com.bugsnag.android

import java.io.IOException
import java.util.Date

/**
 * In order to understand what happened in your application before each crash, it can be helpful
 * to leave short log statements that we call breadcrumbs. Breadcrumbs are
 * attached to a crash to help diagnose what events lead to the error.
 */
internal class BreadcrumbInternal internal constructor(
    var message: String,
    var type: BreadcrumbType,
    var metadata: MutableMap<String, Any?>?,
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
