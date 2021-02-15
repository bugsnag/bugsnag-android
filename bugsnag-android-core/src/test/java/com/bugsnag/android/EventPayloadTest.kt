package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class EventPayloadTest {

    @Test
    fun testCloneNotifier() {
        val original = Notifier()
        val config = BugsnagTestUtils.generateImmutableConfig()
        val payload = EventPayload("api-key", null, null, original, config)
        val copy = payload.notifier
        assertNotSame(original, copy)
        assertNotSame(original.dependencies, copy.dependencies)
        assertEquals(original.dependencies, copy.dependencies)
        assertEquals(original.name, copy.name)
        assertEquals(original.url, copy.url)
        assertEquals(original.version, copy.version)
    }
}
