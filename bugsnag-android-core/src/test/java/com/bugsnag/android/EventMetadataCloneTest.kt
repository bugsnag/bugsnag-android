package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import java.util.Collections
import java.util.HashMap

class EventMetadataCloneTest {

    @Test
    fun testMetadataClone() {
        val data = Metadata()
        data.addMetadata("test_section", "foo", "bar")

        val handledState = HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION)
        val config = BugsnagTestUtils.generateImmutableConfig()
        val event = Event(RuntimeException(), config, handledState, data, NoopLogger)
        event.addMetadata("test_section", "second", "another value")

        // metadata object should be deep copied
        assertNotSame(data, event.impl.metadata)

        // validate event metadata
        val origExpected = mapOf(Pair("foo", "bar"))
        assertEquals(origExpected, data.getMetadata("test_section"))

        // validate event metadata
        val eventExpected = mapOf(Pair("foo", "bar"), Pair("second", "another value"))
        assertEquals(eventExpected, event.impl.metadata.getMetadata("test_section"))
    }
}
