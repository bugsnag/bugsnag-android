package com.bugsnag.android

import java.io.File
import java.io.IOException

class SessionPayload internal constructor(
    internal val session: Session?,
    private val files: List<File>?,
    app: AppData,
    device: DeviceData
) : JsonStream.Streamable {

    val app: MutableMap<String, Any> = app.appDataSummary
    val device: MutableMap<String, Any> = device.deviceDataSummary

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
