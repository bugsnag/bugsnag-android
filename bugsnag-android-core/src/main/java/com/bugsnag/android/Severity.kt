package com.bugsnag.android

import java.io.IOException

/**
 * The severity of an Event, one of "error", "warning" or "info".
 *
 * By default, unhandled exceptions will be Severity.ERROR and handled
 * exceptions sent with bugsnag.notify will be Severity.WARNING.
 */
enum class Severity(internal val str: String) : JsonStream.Streamable {
    ERROR("error"),
    WARNING("warning"),
    INFO("info");

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.value(str)
    }
}
