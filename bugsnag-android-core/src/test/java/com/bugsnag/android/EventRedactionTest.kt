package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import com.bugsnag.android.BugsnagTestUtils.generateAppWithState
import org.junit.Test
import java.io.StringWriter
import java.util.Date

internal class EventRedactionTest {

    @Test
    fun testEventRedaction() {
        val event = Event(
            null,
            generateImmutableConfig(),
            HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION)
        )
        event.app = generateAppWithState()

        event.addMetadata("app", "password", "foo")
        event.addMetadata("device", "password", "bar")
        event.metadata.addMetadata("baz", "password", "hunter2")
        val metadata = mutableMapOf<String, Any?>(Pair("password", "whoops"))
        event.breadcrumbs = listOf(Breadcrumb("Whoops", BreadcrumbType.LOG, metadata, Date(0)))
        event.threads = emptyList()


        val writer = StringWriter()
        val stream = JsonStream(writer)
        event.toStream(stream)
        validateJson("event_redaction.json", writer.toString())
    }
}
