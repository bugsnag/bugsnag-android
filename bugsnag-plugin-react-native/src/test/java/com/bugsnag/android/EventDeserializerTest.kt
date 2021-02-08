package com.bugsnag.android

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

        `when`(client.config).thenReturn(TestData.generateConfig())
        `when`(client.getLogger()).thenReturn(object : Logger {})
        `when`(client.getMetadataState()).thenReturn(TestHooks.generateMetadataState())
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
}
