package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.HashMap

class ErrorDeserializerTest {

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
        map["errorClass"] = "BrowserException"
        map["errorMessage"] = "whoops!"
        map["type"] = "reactnativejs"
    }

    @Test
    fun deserialize() {
        val error = ErrorDeserializer(StackframeDeserializer(), object : Logger {}).deserialize(map)
        assertEquals("BrowserException", error.errorClass)
        assertEquals("whoops!", error.errorMessage)
        assertEquals(ErrorType.REACTNATIVEJS, error.type)

        val frame = error.stacktrace[0]
        assertEquals("foo()", frame.method)
        assertEquals("Bar.kt", frame.file)
        assertEquals(29, frame.lineNumber)
        assertTrue(frame.inProject as Boolean)
    }
}
