package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NativeStackDeserializerTest {

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
            ),
            mapOf(
                "methodName" to "invokeWham",
                "lineNumber" to 159,
                "file" to "Wham.kt"
            )
        )
        map = mapOf(
            "errors" to listOf(error),
            "nativeStack" to nativeStack
        )
    }

    @Test
    fun deserialize() {
        val packages = listOf("com.reactnativetest")
        val cfg = TestData.generateConfig()
        val nativeStack = NativeStackDeserializer(packages, cfg).deserialize(map)
        assertEquals(3, nativeStack.size)

        val firstFrame = nativeStack[0]
        assertEquals("com.reactnativetest.BenCrash.asyncReject", firstFrame.method)
        assertEquals("BenCrash.java", firstFrame.file)
        assertEquals(42, firstFrame.lineNumber)
        assertTrue(firstFrame.inProject!!)
        assertEquals(ErrorType.ANDROID, firstFrame.type)

        val secondFrame = nativeStack[1]
        assertEquals("com.example.Foo.invokeFoo", secondFrame.method)
        assertEquals("Foo.kt", secondFrame.file)
        assertEquals(57, secondFrame.lineNumber)
        assertNull(secondFrame.inProject)
        assertEquals(ErrorType.ANDROID, secondFrame.type)

        val thirdFrame = nativeStack[2]
        assertEquals("invokeWham", thirdFrame.method)
        assertEquals("Wham.kt", thirdFrame.file)
        assertEquals(159, thirdFrame.lineNumber)
        assertNull(secondFrame.inProject)
        assertEquals(ErrorType.ANDROID, thirdFrame.type)
    }
}
