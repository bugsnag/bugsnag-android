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

    private val map = mutableMapOf<String, Any?>()

    /**
     * Generates a map for verifying the serializer
     */
    @Before
    fun setup() {
        map["severity"] = "info"
        map["unhandled"] = false
        map["context"] = "Foo"
        map["groupingHash"] = "SomeHash"
        map["severityReason"] = mapOf(Pair("type", SeverityReason.REASON_HANDLED_EXCEPTION))
        map["user"] = mapOf(Pair("id", "123"))
        map["breadcrumbs"] = listOf(breadcrumbMap())
        map["threads"] = listOf(threadMap())
        map["errors"] = listOf(errorMap())
        map["metadata"] = metadataMap()
        map["app"] = mapOf(Pair("id", "app-id"))
        map["device"] =
            mapOf(Pair("id", "device-id"), Pair("runtimeVersions", mutableMapOf<String, Any>()))
        map["correlation"] = mapOf(
            "traceId" to "b39e53513eec3c68b5e5c34dc43611e0",
            "spanId" to "51d886b3a693a406"
        )

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

    @Test
    fun deserialize() {
        val event = EventDeserializer(client, emptyList()).deserialize(map)
        assertNotNull(event)
        assertEquals(Severity.INFO, event.severity)
        assertFalse(event.isUnhandled)
        assertFalse(TestHooks.getUnhandledOverridden(event))
        assertEquals("Foo", event.context)
        assertEquals("SomeHash", event.groupingHash)
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
        val map: MutableMap<String, Any?> = hashMapOf(
            "unhandled" to false,
            "severityReason" to hashMapOf(
                "type" to "unhandledException",
                "unhandledOverridden" to true
            )
        )
        map["severity"] = "info"
        map["user"] = mapOf(Pair("id", "123"))
        map["breadcrumbs"] = listOf(breadcrumbMap())
        map["threads"] = listOf(threadMap())
        map["errors"] = listOf(errorMap())
        map["metadata"] = metadataMap()
        map["app"] = mapOf(Pair("id", "app-id"))
        map["device"] =
            mapOf(Pair("id", "device-id"), Pair("runtimeVersions", mutableMapOf<String, Any>()))

        val event = EventDeserializer(client, emptyList()).deserialize(map)
        assertFalse(event.isUnhandled)
        assertTrue(TestHooks.getUnhandledOverridden(event))
    }

    @Test
    fun deserializeApiKeyOverridden() {
        val map: MutableMap<String, Any?> = hashMapOf(
            "apiKey" to "abc123",
            "severity" to "info",
            "user" to mapOf("id" to "123"),
            "unhandled" to false,
            "severityReason" to hashMapOf(
                "type" to "unhandledException",
                "unhandledOverridden" to true
            ),
            "breadcrumbs" to listOf(breadcrumbMap()),
            "threads" to listOf(threadMap()),
            "errors" to listOf(errorMap()),
            "metadata" to metadataMap(),
            "app" to mapOf("id" to "app-id"),
            "device" to mapOf(
                "id" to "device-id",
                "runtimeVersions" to hashMapOf<String, Any>()
            )
        )

        val event = EventDeserializer(client, emptyList()).deserialize(map)
        assertEquals("abc123", event.apiKey)
    }
}
