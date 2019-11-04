package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.StringWriter

class JsonStreamTest {

    @Test
    fun placeholderObject() {
        val writer = StringWriter()
        val stream = JsonStream(writer)
        stream.beginObject()
        stream.name("foo").value(Object())
        stream.endObject()
        assertEquals("{\"foo\":\"[OBJECT]\"}", writer.toString())
    }

    @Test
    fun redactedObject() {
        val writer = StringWriter()
        val stream = JsonStream(writer)
        stream.beginObject()
        stream.name("foo").value(mapOf(Pair("password", 5)))
        stream.endObject()
        assertEquals("{\"foo\":{\"password\":\"[REDACTED]\"}}", writer.toString())
    }
}
