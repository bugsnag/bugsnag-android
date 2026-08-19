package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.HashMap

class ErrorDeserializerTest {

    private fun createErrorMap() = HashMap<String, Any>().apply {
        val frame = HashMap<String, Any>()
        frame["method"] = "foo()"
        frame["file"] = "Bar.kt"
        frame["lineNumber"] = 29
        frame["inProject"] = true
        this["stacktrace"] = listOf(frame)
        this["errorClass"] = "BrowserException"
        this["errorMessage"] = "whoops!"
        this["type"] = "reactnativejs"
    }

    private fun createNativeStackFrames(): List<Map<String, Any>> = listOf(
        mapOf(
            "methodName" to "nativeMethod1",
            "lineNumber" to 100,
            "fileName" to "Native.java",
            "className" to "com.reactnativetest.Native"
        ),
        mapOf(
            "methodName" to "nativeMethod2",
            "lineNumber" to 200,
            "fileName" to "NativeHelper.kt",
            "className" to "com.example.NativeHelper"
        )
    )

    @Test
    fun deserializeWithoutNativeStack() {
        val map = createErrorMap()
        val packages = listOf("com.reactnativetest")
        val cfg = TestData.generateConfig()
        val nativeStackDeserializer = NativeStackDeserializer(packages, cfg)
        val errorDeserializer = ErrorDeserializer(
            StackframeDeserializer(),
            nativeStackDeserializer,
            object : Logger {}
        )
        val error = errorDeserializer.deserialize(map)

        assertEquals("BrowserException", error.errorClass)
        assertEquals("whoops!", error.errorMessage)
        assertEquals(ErrorType.REACTNATIVEJS, error.type)
        assertEquals(1, error.stacktrace.size)

        val jsFrame = error.stacktrace[0]
        assertEquals("foo()", jsFrame.method)
        assertEquals("Bar.kt", jsFrame.file)
        assertEquals(29, jsFrame.lineNumber)
        assertTrue(jsFrame.inProject as Boolean)
    }

    @Test
    fun deserializeWithNativeStack() {
        val map = createErrorMap()
        map["nativeStack"] = createNativeStackFrames()

        val packages = listOf("com.reactnativetest")
        val cfg = TestData.generateConfig()
        val nativeStackDeserializer = NativeStackDeserializer(packages, cfg)
        val errorDeserializer = ErrorDeserializer(StackframeDeserializer(), nativeStackDeserializer, object : Logger {})
        val error = errorDeserializer.deserialize(map)

        assertEquals("BrowserException", error.errorClass)
        assertEquals("whoops!", error.errorMessage)
        assertEquals(ErrorType.REACTNATIVEJS, error.type)

        // Should have 3 frames total: 2 native frames + 1 JS frame
        assertEquals(3, error.stacktrace.size)

        // Native frames should be at the start (indices 0 and 1)
        val firstNativeFrame = error.stacktrace[0]
        assertEquals("com.reactnativetest.Native.nativeMethod1", firstNativeFrame.method)
        assertEquals("Native.java", firstNativeFrame.file)
        assertEquals(100, firstNativeFrame.lineNumber)
        assertTrue(firstNativeFrame.inProject!!)
        assertEquals(ErrorType.ANDROID, firstNativeFrame.type)

        val secondNativeFrame = error.stacktrace[1]
        assertEquals("com.example.NativeHelper.nativeMethod2", secondNativeFrame.method)
        assertEquals("NativeHelper.kt", secondNativeFrame.file)
        assertEquals(200, secondNativeFrame.lineNumber)
        assertNull(secondNativeFrame.inProject)
        assertEquals(ErrorType.ANDROID, secondNativeFrame.type)

        // Original JS frame should now be at index 2
        val jsFrame = error.stacktrace[2]
        assertEquals("foo()", jsFrame.method)
        assertEquals("Bar.kt", jsFrame.file)
        assertEquals(29, jsFrame.lineNumber)
        assertTrue(jsFrame.inProject as Boolean)
    }
}
