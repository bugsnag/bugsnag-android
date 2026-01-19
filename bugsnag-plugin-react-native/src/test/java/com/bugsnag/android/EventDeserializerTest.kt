package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.util.Date
import java.util.UUID

@RunWith(MockitoJUnitRunner::class)
class EventDeserializerTest {

    @Mock
    lateinit var client: Client

    @Before
    fun setup() {
        `when`(client.config).thenReturn(TestData.generateConfig())
        `when`(client.getLogger()).thenReturn(object : Logger {})
        `when`(client.getMetadataState()).thenReturn(TestHooks.generateMetadataState())
        `when`(client.getFeatureFlagState()).thenReturn(TestHooks.generateFeatureFlagsState())
    }

    private fun breadcrumbMap() = hashMapOf(
        "message" to "Whoops",
        "type" to "navigation",
        "timestamp" to DateUtils.toIso8601(Date(0))
    )

    private fun threadMap() = hashMapOf(
        "stacktrace" to listOf<Any>(),
        "id" to 52L,
        "type" to "reactnativejs",
        "name" to "thread-worker-02",
        "state" to "RUNNABLE",
        "errorReportingThread" to true
    )

    private fun errorMap() = hashMapOf(
        "stacktrace" to emptyList<Any>(),
        "errorClass" to "BrowserException",
        "errorMessage" to "whoops!",
        "type" to "reactnativejs"
    )

    private fun metadataMap() = hashMapOf(
        "custom" to hashMapOf(
            "id" to "123"
        )
    )

    private fun baseEventMap() = mutableMapOf<String, Any?>(
        "severity" to "info",
        "unhandled" to false,
        "context" to "Foo",
        "groupingHash" to "SomeHash",
        "groupingDiscriminator" to "SomeDiscriminator",
        "severityReason" to mapOf("type" to SeverityReason.REASON_HANDLED_EXCEPTION),
        "user" to mapOf("id" to "123"),
        "breadcrumbs" to listOf(breadcrumbMap()),
        "threads" to listOf(threadMap()),
        "errors" to listOf(errorMap()),
        "metadata" to metadataMap(),
        "app" to mapOf("id" to "app-id"),
        "device" to mapOf("id" to "device-id", "runtimeVersions" to mutableMapOf<String, Any>()),
        "correlation" to mapOf(
            "traceId" to "b39e53513eec3c68b5e5c34dc43611e0",
            "spanId" to "51d886b3a693a406"
        )
    )

    private fun oldNativeStackEventMap() = baseEventMap().apply {
        this["errors"] = listOf(
            hashMapOf(
                "stacktrace" to listOf(
                    hashMapOf(
                        "method" to "jsFunction",
                        "file" to "App.js",
                        "lineNumber" to 50,
                        "inProject" to true
                    )
                ),
                "errorClass" to "Error",
                "errorMessage" to "Something went wrong",
                "type" to "reactnativejs"
            )
        )
        this["nativeStack"] = listOf(
            mapOf(
                "methodName" to "nativeMethod",
                "lineNumber" to 42,
                "fileName" to "Native.java",
                "className" to "com.reactnativetest.Native"
            ),
            mapOf(
                "methodName" to "helperMethod",
                "lineNumber" to 99,
                "fileName" to "Helper.kt",
                "className" to "com.example.Helper"
            )
        )
    }

    private fun newNativeStackEventMap() = baseEventMap().apply {
        this["errors"] = listOf(
            // First error without nativeStack
            hashMapOf(
                "stacktrace" to listOf(
                    hashMapOf(
                        "method" to "firstError",
                        "file" to "First.js",
                        "lineNumber" to 10,
                        "inProject" to true
                    )
                ),
                "errorClass" to "FirstError",
                "errorMessage" to "First error",
                "type" to "reactnativejs"
            ),
            // Second error with nativeStack
            hashMapOf(
                "stacktrace" to listOf(
                    hashMapOf(
                        "method" to "secondError",
                        "file" to "Second.js",
                        "lineNumber" to 20,
                        "inProject" to true
                    )
                ),
                "errorClass" to "SecondError",
                "errorMessage" to "Second error",
                "type" to "reactnativejs",
                "nativeStack" to listOf(
                    mapOf(
                        "methodName" to "nativeMethod",
                        "lineNumber" to 42,
                        "fileName" to "Native.java",
                        "className" to "com.reactnativetest.Native"
                    ),
                    mapOf(
                        "methodName" to "helperMethod",
                        "lineNumber" to 99,
                        "fileName" to "Helper.kt",
                        "className" to "com.example.Helper"
                    )
                )
            )
        )
    }

    @Test
    fun deserialize() {
        val map = baseEventMap()
        val event = EventDeserializer(client, emptyList()).deserialize(map)
        assertNotNull(event)
        assertEquals(Severity.INFO, event.severity)
        assertFalse(event.isUnhandled)
        assertFalse(TestHooks.getUnhandledOverridden(event))
        assertEquals("Foo", event.context)
        assertEquals("SomeHash", event.groupingHash)
        assertEquals("SomeDiscriminator", event.groupingDiscriminator)
        assertEquals("123", event.getUser().id)
        assertTrue(event.breadcrumbs.isNotEmpty())
        assertTrue(event.threads.isNotEmpty())
        assertTrue(event.errors.isNotEmpty())
        assertEquals("app-id", event.app.id)
        assertEquals("device-id", event.device.id)
        assertEquals("123", event.getMetadata("custom", "id"))
        assertEquals(TestData.generateConfig().apiKey, event.apiKey)

        assertEquals(
            UUID(-5503870086187041688L, -5339647044406079008L),
            TestHooks.getCorrelatedTraceId(event)
        )

        assertEquals(5897611818193626118L, TestHooks.getCorrelatedSpanId(event))
    }

    @Test
    fun deserializeUnhandledOverridden() {
        val map = baseEventMap()
        map["unhandled"] = false
        map["severityReason"] = hashMapOf(
            "type" to "unhandledException",
            "unhandledOverridden" to true
        )

        val event = EventDeserializer(client, emptyList()).deserialize(map)
        assertFalse(event.isUnhandled)
        assertTrue(TestHooks.getUnhandledOverridden(event))
    }

    @Test
    fun deserializeApiKeyOverridden() {
        val map = baseEventMap()
        map["apiKey"] = "abc123"

        val event = EventDeserializer(client, emptyList()).deserialize(map)
        assertEquals("abc123", event.apiKey)
    }

    @Test
    fun deserializeOldStyleNativeStack() {
        // Old style: nativeStack at top level of event, single error
        val eventMap = oldNativeStackEventMap()
        val event = EventDeserializer(client, listOf("com.reactnativetest")).deserialize(eventMap)
        assertEquals(1, event.errors.size)

        val error = event.errors[0]
        // Should have 3 frames: 2 native + 1 JS
        assertEquals(3, error.stacktrace.size)

        // Native frames should be at the start
        val firstNativeFrame = error.stacktrace[0]
        assertEquals("com.reactnativetest.Native.nativeMethod", firstNativeFrame.method)
        assertEquals("Native.java", firstNativeFrame.file)
        assertEquals(42, firstNativeFrame.lineNumber)
        assertEquals(ErrorType.ANDROID, firstNativeFrame.type)
        assertTrue(firstNativeFrame.inProject!!)

        val secondNativeFrame = error.stacktrace[1]
        assertEquals("com.example.Helper.helperMethod", secondNativeFrame.method)
        assertEquals("Helper.kt", secondNativeFrame.file)
        assertEquals(99, secondNativeFrame.lineNumber)
        assertEquals(ErrorType.ANDROID, secondNativeFrame.type)

        // Original JS frame should be at index 2
        val jsFrame = error.stacktrace[2]
        assertEquals("jsFunction", jsFrame.method)
        assertEquals("App.js", jsFrame.file)
        assertEquals(50, jsFrame.lineNumber)
    }

    @Test
    fun deserializeNewStyleNativeStack() {
        // New style: nativeStack per error, multiple errors
        val eventMap = newNativeStackEventMap()
        val event = EventDeserializer(client, listOf("com.reactnativetest")).deserialize(eventMap)
        assertEquals(2, event.errors.size)

        // First error should have only its original frame
        val firstError = event.errors[0]
        assertEquals(1, firstError.stacktrace.size)
        assertEquals("firstError", firstError.stacktrace[0].method)
        assertEquals("First.js", firstError.stacktrace[0].file)

        // Second error should have native frames prepended
        val secondError = event.errors[1]
        assertEquals(3, secondError.stacktrace.size)

        // Native frames should be at the start
        val firstNativeFrame = secondError.stacktrace[0]
        assertEquals("com.reactnativetest.Native.nativeMethod", firstNativeFrame.method)
        assertEquals("Native.java", firstNativeFrame.file)
        assertEquals(42, firstNativeFrame.lineNumber)
        assertEquals(ErrorType.ANDROID, firstNativeFrame.type)
        assertTrue(firstNativeFrame.inProject!!)

        val secondNativeFrame = secondError.stacktrace[1]
        assertEquals("com.example.Helper.helperMethod", secondNativeFrame.method)
        assertEquals("Helper.kt", secondNativeFrame.file)
        assertEquals(99, secondNativeFrame.lineNumber)
        assertEquals(ErrorType.ANDROID, secondNativeFrame.type)

        // Original JS frame should be at index 2
        val jsFrame = secondError.stacktrace[2]
        assertEquals("secondError", jsFrame.method)
        assertEquals("Second.js", jsFrame.file)
        assertEquals(20, jsFrame.lineNumber)
    }
}
