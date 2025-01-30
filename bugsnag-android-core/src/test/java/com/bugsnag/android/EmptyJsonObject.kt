package com.bugsnag.android

import com.bugsnag.android.JsonStream.Streamable

/**
 * Test implementation of [Streamable] which writes only an empty JSON object.
 */
internal object EmptyJsonObject : Streamable {
    override fun toStream(stream: JsonStream) {
        stream.beginObject()
            .endObject()
    }
}
