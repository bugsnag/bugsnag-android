package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import org.junit.Assert.assertFalse
import org.junit.Test

class AnrConfigTest {

    private val config = generateConfiguration()

    @Test
    fun testDetectAnrDefault() {
        assertFalse(config.autoDetectAnrs)
    }
}
