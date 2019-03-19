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
     * Verifies that attempts to set the ANR threshold below 1000ms set the value as 1000ms
     */
    @Test
    fun testAnrThresholdMs() {
        val config = config
        assertEquals(5000, config.anrThresholdMs)

        config.anrThresholdMs = 10000
        assertEquals(10000, config.anrThresholdMs)

        arrayOf(1000, 999, 0, -5, Long.MIN_VALUE).forEach {
            config.anrThresholdMs = it
            assertEquals(1000, config.anrThresholdMs)
        }
    }
}
