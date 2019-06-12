package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AnrConfigTest {

    private val config = Configuration("api-key")

    @Test
    fun testDetectAnrDefault() {
        assertFalse(config.detectAnrs)
    }
}
