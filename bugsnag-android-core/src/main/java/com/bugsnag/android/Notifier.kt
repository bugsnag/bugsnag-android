package com.bugsnag.android

import java.io.IOException

/**
 * Information about this library, including name and version.
 */
internal object Notifier : JsonStream.Streamable {

    var name: String = "Android Bugsnag Notifier"
    var version: String = "4.21.0"
    var url: String = "https://bugsnag.com"

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("name").value(name)
        writer.name("version").value(version)
        writer.name("url").value(url)
        writer.endObject()
    }
}
