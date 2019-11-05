package com.bugsnag.android

import java.io.IOException

class Stackframe internal constructor(
    var method: String?,
    var file: String?,
    var lineNumber: Number?,
    var inProject: Boolean?,
    private var customFields: Map<String, Any?>
): JsonStream.Streamable {

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("method").value(method)
        writer.name("file").value(file)
        writer.name("lineNumber").value(lineNumber)
        writer.name("inProject").value(inProject)
        customFields.forEach {
            writer.name(it.key)
            writer.value(it.value)
        }
        writer.endObject()
    }
}
