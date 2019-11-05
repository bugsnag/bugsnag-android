package com.bugsnag.android

import java.io.File
import java.io.IOException

class SessionPayload internal constructor(
    internal val session: Session?,
    private val files: List<File>?,
    var app: MutableMap<String, Any?>,
    var device: MutableMap<String, Any?>
) : JsonStream.Streamable {

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("notifier").value(Notifier)
        writer.name("app").value(app)
        writer.name("device").value(device)
        writer.name("sessions").beginArray()

        if (files != null) {
            for (file in files) {
                writer.value(file)
            }
        } else if (session != null) {
            writer.value(session)
        }

        writer.endArray()
        writer.endObject()
    }
}
