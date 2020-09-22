package com.bugsnag.android

import com.bugsnag.android.TestData.generateError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NativeErrorDeserializerTest {

    private lateinit var map: Map<String, Any>

    /**
     * Generates a map for verifying the serializer
     */
    @Before
    fun setup() {
        val errorStacktrace = listOf(
            mapOf(
                "method" to "foo()",
                "file" to "Bar.kt",
                "lineNumber" to 29,
                "inProject" to true
            )
        )
        val error = mapOf(
            "stacktrace" to errorStacktrace,
            "errorClass" to "BrowserException",
            "errorMessage" to "whoops!",
            "type" to "reactnativejs"
        )

        val nativeStack = listOf(
            mapOf(
                "methodName" to "asyncReject",
                "lineNumber" to 42,
                "file" to "BenCrash.java",
                "class" to "com.reactnativetest.BenCrash"
            ),
            mapOf(
                "methodName" to "invokeFoo",
                "lineNumber" to 57,
                "file" to "Foo.kt",
                "class" to "com.example.Foo"
            )
        )
        map = mapOf(
            "errors" to listOf(error),
            "nativeStack" to nativeStack
        )
    }

    @Test
    fun deserialize() {
        val logger = object : Logger {}
        val packages = listOf("com.reactnativetest")
        val error = NativeErrorDeserializer(generateError(), packages, logger).deserialize(map)
        assertEquals("BrowserException", error.errorClass)
        assertEquals("whoops!", error.errorMessage)
        assertEquals(ErrorType.ANDROID, error.type)
        assertEquals(2, error.stacktrace.count())

        val firstFrame = error.stacktrace[0]
        assertEquals("com.reactnativetest.BenCrash.asyncReject", firstFrame.method)
        assertEquals("BenCrash.java", firstFrame.file)
        assertEquals(42, firstFrame.lineNumber)
        assertTrue(firstFrame.inProject!!)

        val secondFrame = error.stacktrace[1]
        assertEquals("com.example.Foo.invokeFoo", secondFrame.method)
        assertEquals("Foo.kt", secondFrame.file)
        assertEquals(57, secondFrame.lineNumber)
        assertNull(secondFrame.inProject)
    }
}
