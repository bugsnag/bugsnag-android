package com.bugsnag.android

import java.io.IOException
import java.util.Date

/**
 * In order to understand what happened in your application before each crash, it can be helpful
 * to leave short log statements that we call breadcrumbs. Breadcrumbs are
 * attached to a crash to help diagnose what events lead to the error.
 */
class BreadcrumbImpl internal constructor(

    /**
     * The description of the breadcrumb
     */
    var message: String,

    /**
     * The type of breadcrumb left - one of those enabled in [Configuration.enabledBreadcrumbTypes]
     */
    var type: BreadcrumbType,

    /**
     * Diagnostic data relating to the breadcrumb
     */
    var metadata: MutableMap<String, Any?>?,

    /**
     * The timestamp that the breadcrumb was left
     */
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
