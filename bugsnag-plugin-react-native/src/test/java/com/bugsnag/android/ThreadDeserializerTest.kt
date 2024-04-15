package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadDeserializerTest {

    @Test
    fun deserialize() {
        val thread = ThreadDeserializer(StackframeDeserializer(), object : Logger {})
            .deserialize(createThreadAsMap())
        assertEquals("thread-id-as-string", thread.id)
        assertCommonThreadContent(thread)
    }

    @Test
    fun deserializeLegacyThread() {
        val thread =
            ThreadDeserializer(StackframeDeserializer(), object : Logger {})
                .deserialize(createLegacyThreadAsMap())
        assertEquals("52", thread.id)
        assertCommonThreadContent(thread)
    }

    private fun assertCommonThreadContent(thread: Thread) {
        assertEquals(ErrorType.REACTNATIVEJS, thread.type)
        assertEquals("thread-worker-02", thread.name)
        assertTrue(thread.errorReportingThread)

        val frame = thread.stacktrace[0]
        assertEquals("foo()", frame.method)
        assertEquals("Bar.kt", frame.file)
        assertEquals(29, frame.lineNumber)
        assertTrue(frame.inProject as Boolean)
    }

    private fun createLegacyThreadAsMap(): Map<String, Any> = hashMapOf(
        "stacktrace" to listOf(
            hashMapOf(
                "method" to "foo()",
                "file" to "Bar.kt",
                "lineNumber" to 29,
                "inProject" to true
            )
        ),
        "id" to 52L,
        "type" to "reactnativejs",
        "name" to "thread-worker-02",
        "state" to "RUNNABLE",
        "errorReportingThread" to true
    )

    private fun createThreadAsMap(): Map<String, Any> =
        createLegacyThreadAsMap() + mapOf("id" to "thread-id-as-string")
}
