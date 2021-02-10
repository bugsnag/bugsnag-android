package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateAppWithState
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
            generateImmutableConfig(),
            SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
            NoopLogger
        )
        event.app = generateAppWithState()
        event.device = generateDeviceWithState()

        event.addMetadata("app", "password", "foo")
        event.addMetadata("device", "password", "bar")
        event.impl.metadata.addMetadata("baz", "password", "hunter2")
        val metadata = mutableMapOf<String, Any?>(Pair("password", "whoops"))
        event.breadcrumbs = listOf(Breadcrumb("Whoops", BreadcrumbType.LOG, metadata, Date(0), NoopLogger))
        event.threads.clear()
        event.device.cpuAbi = emptyArray()

        val writer = StringWriter()
        val stream = JsonStream(writer)
        event.toStream(stream)
        validateJson("event_redaction.json", writer.toString())
    }
}
