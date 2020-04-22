package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.HashMap

class StackframeDeserializerTest {

    private val map = HashMap<String, Any>()

    /**
     * Generates a map for verifying the serializer
     */
    @Before
    fun setup() {
        map["method"] = "foo()"
        map["file"] = "Bar.kt"
        map["lineNumber"] = 29
        map["inProject"] = true
        map["columnNumber"] = 52
        map["code"] = hashMapOf(
            "55" to "foo(bar)",
            "56" to "var x = 5;"
        )
    }

    @Test
    fun deserialize() {
        val frame = StackframeDeserializer().deserialize(map)
        assertEquals("foo()", frame.method)
        assertEquals("Bar.kt", frame.file)
        assertEquals(29, frame.lineNumber)
        assertEquals(52, frame.columnNumber)
        assertEquals("foo(bar)", frame.code!!["55"])
        assertEquals("var x = 5;", frame.code!!["56"])
        assertTrue(frame.inProject as Boolean)
    }
}
