package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnrConfigTest {

    private val config = Configuration("api-key")

    @Test
    fun testDetectAnrDefault() {
        assertTrue(config.detectAnrs)
    }

    /**
     * Verifies that attempts to set the ANR threshold below 100ms set the value as 100ms
     */
    @Test
    fun testAnrThresholdMs() {
        val config = config
        assertEquals(5000, config.anrThresholdMs)

        config.anrThresholdMs = 10000
        assertEquals(10000, config.anrThresholdMs)

        arrayOf(100, 99, 0, -5, Long.MIN_VALUE).forEach {
            config.anrThresholdMs = it
            assertEquals(100, config.anrThresholdMs)
        }
    }
}
