package com.bugsnag.android

import com.bugsnag.android.ErrorCaptureOptions.Companion.CAPTURE_ALL
import com.bugsnag.android.ErrorCaptureOptions.Companion.CAPTURE_BREADCRUMBS
import com.bugsnag.android.ErrorCaptureOptions.Companion.CAPTURE_FEATURE_FLAGS
import com.bugsnag.android.ErrorCaptureOptions.Companion.CAPTURE_STACKTRACE
import com.bugsnag.android.ErrorCaptureOptions.Companion.CAPTURE_THREADS
import com.bugsnag.android.ErrorCaptureOptions.Companion.CAPTURE_USER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorCaptureOptionsTest {

    @Test
    fun testDefaultValues() {
        val options = ErrorCaptureOptions()
        assertTrue(options.breadcrumbs)
        assertTrue(options.featureFlags)
        assertNull(options.metadata)
        assertTrue(options.stacktrace)
        assertTrue(options.threads)
        assertTrue(options.user)
    }

    @Test
    fun testCaptureOnlyStacktraceAndUser() {
        val fields = CAPTURE_STACKTRACE or CAPTURE_USER
        val options = ErrorCaptureOptions.captureOnly(fields)
        assertTrue(options.stacktrace)
        assertFalse(options.breadcrumbs)
        assertFalse(options.featureFlags)
        assertFalse(options.threads)
        assertTrue(options.user)
        assertNull(options.metadata)
    }

    @Test
    fun testCaptureOnlyWithMetadata() {
        val fields = CAPTURE_BREADCRUMBS or CAPTURE_FEATURE_FLAGS
        val metadataTabs = setOf("customTab")
        val options = ErrorCaptureOptions.captureOnly(fields, metadataTabs)
        assertFalse(options.stacktrace)
        assertTrue(options.breadcrumbs)
        assertTrue(options.featureFlags)
        assertFalse(options.threads)
        assertFalse(options.user)
        assertEquals(metadataTabs, options.metadata)
    }

    @Test
    fun testCaptureNothing() {
        val options = ErrorCaptureOptions.captureNothing()
        assertFalse(options.stacktrace)
        assertFalse(options.breadcrumbs)
        assertFalse(options.featureFlags)
        assertFalse(options.threads)
        assertFalse(options.user)
        assertEquals(emptySet<String>(), options.metadata)
    }

    @Test
    fun captureOnlyBitFields() {
        val bitMappings = mapOf<Int, (ErrorCaptureOptions) -> Boolean>(
            CAPTURE_STACKTRACE to ErrorCaptureOptions::stacktrace,
            CAPTURE_BREADCRUMBS to ErrorCaptureOptions::breadcrumbs,
            CAPTURE_FEATURE_FLAGS to ErrorCaptureOptions::featureFlags,
            CAPTURE_THREADS to ErrorCaptureOptions::threads,
            CAPTURE_USER to ErrorCaptureOptions::user,
        )

        // test every possible combination of bit fields -> Boolean mappings
        val allBits = bitMappings.keys.toIntArray()
        val totalCombinations = 1 shl allBits.size

        for (combination in 0 until totalCombinations) {
            var fields = 0
            val expectedEnabled = mutableSetOf<Int>()

            // Build the bit field based on the combination
            for (i in allBits.indices) {
                if ((combination and (1 shl i)) != 0) {
                    fields = fields or allBits[i]
                    expectedEnabled.add(allBits[i])
                }
            }

            val options = ErrorCaptureOptions.captureOnly(fields)

            // Verify each field matches expectations
            for ((bit, getter) in bitMappings) {
                val shouldBeEnabled = bit in expectedEnabled
                assertEquals(
                    "Combination $combination: bit $bit should be $shouldBeEnabled",
                    shouldBeEnabled,
                    getter(options)
                )
            }
        }
    }

    @Test
    fun testCaptureAll() {
        val options = ErrorCaptureOptions.captureOnly(CAPTURE_ALL)
        assertTrue("breadcrumbs", options.breadcrumbs)
        assertTrue("featureFlags", options.featureFlags)
        assertTrue("stacktrace", options.stacktrace)
        assertTrue("threads", options.threads)
        assertTrue("user", options.user)
        assertNull(options.metadata)
    }
}
