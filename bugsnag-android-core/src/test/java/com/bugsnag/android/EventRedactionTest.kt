package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateAppWithState
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateDeviceWithState
import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Test
import java.io.StringWriter
import java.util.Date

internal class EventRedactionTest {

    @Test
    fun testEventRedaction() {
        val event = Event(
            null,
            generateImmutableConfig(
                generateConfiguration().apply {
                    redactedKeys = setOf(
                        ".*password.*".toPattern(),
                        ".*changeme.*".toPattern()
                    )

                    projectPackages = setOf(
                        "com.example.foo"
                    )
                }
            ),
            SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
            NoopLogger
        )

        testEventRedaction(event, "event_redaction.json")
    }

    @Test
    fun testDefaultEventRedaction() {
        val event = Event(
            null,
            generateImmutableConfig(
                generateConfiguration().apply {
                    projectPackages = setOf(
                        "com.example.foo"
                    )
                }
            ),
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
        event.impl.metadata.addMetadata("baz", "password", "hunter2")
        val metadata = mutableMapOf<String, Any?>(Pair("changeme", "whoops"))
        event.breadcrumbs =
            listOf(Breadcrumb("Whoops", BreadcrumbType.LOG, metadata, Date(0), NoopLogger))
        event.threads.clear()
        event.device.cpuAbi = emptyArray()

        val writer = StringWriter()
        val stream = JsonStream(writer)
        event.toStream(stream)
        validateJson(jsonFixture, writer.toString())
    }
}
