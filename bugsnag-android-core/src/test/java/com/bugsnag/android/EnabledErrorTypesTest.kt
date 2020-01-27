package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnabledErrorTypesTest {

    private val config = generateConfiguration()

    @Test
    fun testDetectAnrDefault() {
        assertTrue(config.autoDetectErrors)
        assertTrue(config.enabledErrorTypes.anrs)
        assertFalse(config.enabledErrorTypes.ndkCrashes)
        assertTrue(config.enabledErrorTypes.unhandledExceptions)
    }

    @Test
    fun autoDetectErrorsTrueConfig() {
        config.autoDetectErrors = true

        with(convertToImmutableConfig(config).enabledErrorTypes) {
            assertTrue(anrs)
            assertFalse(ndkCrashes)
            assertTrue(unhandledExceptions)
        }
    }

    @Test
    fun autoDetectErrorsFalseConfig() {
        config.autoDetectErrors = false

        with(convertToImmutableConfig(config).enabledErrorTypes) {
            assertFalse(anrs)
            assertFalse(ndkCrashes)
            assertFalse(unhandledExceptions)
        }
    }

    @Test
    fun errorTypeFalseConstructor() {
        with(ErrorTypes(false)) {
            assertFalse(anrs)
            assertFalse(ndkCrashes)
            assertFalse(unhandledExceptions)
        }
    }
}
