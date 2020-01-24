package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnabledErrorTypesTest {

    private val config = generateConfiguration()

    @Test
    fun testDetectAnrDefault() {
        assertTrue(config.enabledErrorTypes.anrs)
        assertFalse(config.enabledErrorTypes.ndkCrashes)
        assertTrue(config.enabledErrorTypes.unhandledExceptions)
    }
}
