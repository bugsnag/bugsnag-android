package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateAppWithState
import com.bugsnag.android.BugsnagTestUtils.generateDeviceWithState
import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Test
import java.io.StringWriter
import java.util.Date
import java.util.regex.Pattern

internal class EventRedactionTest {

    @Test
    fun testEventRedaction() {
        val event = Event(
            null,
            generateImmutableConfig()
                .copy(redactedKeys = setOf(Pattern.compile(".*password.*"), Pattern.compile(".*changeme.*"))),
            SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
            NoopLogger
        )

        testEventRedaction(event, "event_redaction.json")
    }

    @Test
    fun testDefaultEventRedaction() {
        val event = Event(
            null,
            generateImmutableConfig(),
            SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
            NoopLogger
        )

        testEventRedaction(event, "event_default_redaction.json")
    }

    private fun testEventRedaction(event: Event, jsonFixture: String) {
        event.app = generateAppWithState()
        event.device = generateDeviceWithState()

        event.addMetadata("app", "password", "foo")
        event.addMetadata("device", "password", "bar")
        event.metadata.addMetadata("baz", "password", "hunter2")
        val metadata = mutableMapOf<String, Any?>(Pair("changeme", "whoops"))
        event.breadcrumbs = mutableListOf(Breadcrumb("Whoops", BreadcrumbType.LOG, metadata, Date(0), NoopLogger))
        event.threads.clear()
        event.device.cpuAbi = emptyArray()

        val writer = StringWriter()
        val stream = JsonStream(writer)
        event.toStream(stream)
        validateJson(jsonFixture, writer.toString())
    }
}
