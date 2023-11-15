package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.HashMap

class ThreadDeserializerTest {

    private val map = HashMap<String, Any>()

    /**
     * Generates a map for verifying the serializer
     */
    @Before
    fun setup() {
        val frame = HashMap<String, Any>()
        frame["method"] = "foo()"
        frame["file"] = "Bar.kt"
        frame["lineNumber"] = 29
        frame["inProject"] = true

        map["stacktrace"] = listOf(frame)
        map["id"] = 52L
        map["type"] = "reactnativejs"
        map["name"] = "thread-worker-02"
        map["state"] = "RUNNABLE"
        map["errorReportingThread"] = true
    }

    @Test
    fun deserialize() {
        val thread = ThreadDeserializer(StackframeDeserializer(), object : Logger {}).deserialize(map)
        assertEquals("52", thread.id)
        assertEquals(ErrorType.REACTNATIVEJS, thread.type)
        assertEquals("thread-worker-02", thread.name)
        assertTrue(thread.errorReportingThread)

        val frame = thread.stacktrace[0]
        assertEquals("foo()", frame.method)
        assertEquals("Bar.kt", frame.file)
        assertEquals(29, frame.lineNumber)
        assertTrue(frame.inProject as Boolean)
    }
}
