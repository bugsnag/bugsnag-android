package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.StringWriter
import java.util.Date

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
    fun redactKeyNoRedaction() {
        val writer = StringWriter()
        val stream = JsonStream(writer)
        stream.beginObject()
        stream.name("foo").value(mapOf(Pair("password", 5)), false)
        stream.endObject()
        assertEquals("{\"foo\":{\"password\":5}}", writer.toString())
    }

    @Test
    fun redactKeyWithRedaction() {
        val writer = StringWriter()
        val stream = JsonStream(writer)
        stream.beginObject()
        stream.name("foo").value(mapOf(Pair("password", 5)), true)
        stream.endObject()
        assertEquals("{\"foo\":{\"password\":\"[REDACTED]\"}}", writer.toString())
    }

    @Test
    fun testSaneValues() {
        val writer = StringWriter()
        val stream = JsonStream(writer)
        val breadcrumb = Breadcrumb("whoops", BreadcrumbType.LOG, mutableMapOf(), Date(0))

        stream.beginObject()
        stream.name("bool").value(true)
        stream.name("string").value("string")
        stream.name("int").value(123)
        stream.name("long").value(123L)
        stream.name("float").value(123.45)
        stream.name("streamable").value(breadcrumb)
        stream.name("map").value(mapOf(Pair("Baz", "What")))
        stream.name("collection").value(listOf("bar"))
        stream.name("array").value(arrayOf("foo"))
        stream.name("object").value(Object())
        stream.endObject()
        validateJson("json_stream.json", writer.toString())
    }
}
