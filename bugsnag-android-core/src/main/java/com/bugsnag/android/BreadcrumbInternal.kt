package com.bugsnag.android

import java.io.IOException
import java.util.Date

/**
 * In order to understand what happened in your application before each crash, it can be helpful
 * to leave short log statements that we call breadcrumbs. Breadcrumbs are
 * attached to a crash to help diagnose what events lead to the error.
 */
internal class BreadcrumbInternal internal constructor(
    data: MutableMap<String, Any?> = mutableMapOf()
) : JsonStream.Streamable { // JvmField allows direct field access optimizations

    private val map: MutableMap<String, Any?> = data.withDefault { null }

    val timestamp: Date by map
    var metadata: MutableMap<String, Any?>? by map
    var type: BreadcrumbType by map
    var message: String by map

    internal constructor(message: String) : this(
        message,
        BreadcrumbType.MANUAL,
        mutableMapOf(),
        Date()
    )

    internal constructor(
        message: String,
        type: BreadcrumbType,
        metadata: MutableMap<String, Any?>?,
        timestamp: Date = Date()
    ) : this(
        mutableMapOf(
            "message" to message,
            "type" to type,
            "timestamp" to timestamp,
            "metadata" to metadata
        )
    )

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("timestamp").value(timestamp)
        writer.name("name").value(message)
        writer.name("type").value(type.toString())
        writer.name("metaData")
        writer.value(metadata, true)
        writer.endObject()
    }
}
