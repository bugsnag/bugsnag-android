package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NativeStackDeserializerTest(
    @Suppress("unused")
    private val testName: String,
    private val nativeStackMap: Map<String, Any>
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("Old Architecture", createOldArchNativeStackMap()),
                arrayOf("New Architecture", createNewArchNativeStackMap())
            )
        }

        private fun createOldArchNativeStackMap(): Map<String, Any> = mapOf(
            "nativeStack" to listOf(
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
        )

        private fun createNewArchNativeStackMap(): Map<String, Any> = mapOf(
            "nativeStack" to listOf(
                mapOf(
                    "methodName" to "asyncReject",
                    "lineNumber" to 42,
                    "fileName" to "BenCrash.java",
                    "className" to "com.reactnativetest.BenCrash"
                ),
                mapOf(
                    "methodName" to "invokeFoo",
                    "lineNumber" to 57,
                    "fileName" to "Foo.kt",
                    "className" to "com.example.Foo"
                ),
                mapOf(
                    "methodName" to "invokeWham",
                    "lineNumber" to 159,
                    "fileName" to "Wham.kt"
                )
            )
        )
    }

    @Test
    fun deserializeNativeStack() {
        val packages = listOf("com.reactnativetest")
        val cfg = TestData.generateConfig()
        val nativeStack = NativeStackDeserializer(packages, cfg).deserialize(nativeStackMap)
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
        assertNull(thirdFrame.inProject)
        assertEquals(ErrorType.ANDROID, thirdFrame.type)
    }
}
