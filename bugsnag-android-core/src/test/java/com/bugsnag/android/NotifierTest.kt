package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

import org.junit.Before
import org.junit.Test

class NotifierTest {

    private val notifier = Notifier

    @Test
    fun testName() {
        assertEquals("Android Bugsnag Notifier", notifier.name)
        val expected = "CrossPlatformFramework"
        notifier.name = expected
        assertEquals(expected, notifier.name)
    }

    @Test
    fun testVersion() {
        assertNotNull(notifier.version)
        val expected = "1.2.3"
        notifier.version = expected
        assertEquals(expected, notifier.version)
    }

    @Test
    fun testUrl() {
        assertEquals("https://bugsnag.com", notifier.url)
        val expected = "http://example.com"
        notifier.url = expected
        assertEquals(expected, notifier.url)
    }
}
