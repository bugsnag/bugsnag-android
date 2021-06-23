package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.internal.convertToImmutableConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnabledErrorTypesTest {

    private val config = generateConfiguration()

    @Test
    fun testDetectAnrDefault() {
        assertTrue(config.autoDetectErrors)
        assertTrue(config.enabledErrorTypes.anrs)
        assertTrue(config.enabledErrorTypes.ndkCrashes)
        assertTrue(config.enabledErrorTypes.unhandledExceptions)
        assertTrue(config.enabledErrorTypes.unhandledRejections)
    }

    @Test
    fun autoDetectErrorsTrueConfig() {
        config.autoDetectErrors = true

        with(convertToImmutableConfig(config).enabledErrorTypes) {
            assertTrue(anrs)
            assertTrue(ndkCrashes)
            assertTrue(unhandledExceptions)
            assertTrue(unhandledRejections)
        }
    }

    @Test
    fun autoDetectErrorsFalseConfig() {
        config.autoDetectErrors = false

        with(convertToImmutableConfig(config).enabledErrorTypes) {
            assertFalse(anrs)
            assertFalse(ndkCrashes)
            assertFalse(unhandledExceptions)
            assertFalse(unhandledRejections)
        }
    }

    @Test
    fun errorTypeFalseConstructor() {
        with(ErrorTypes(false)) {
            assertFalse(anrs)
            assertFalse(ndkCrashes)
            assertFalse(unhandledExceptions)
            assertFalse(unhandledRejections)
        }
    }
}
