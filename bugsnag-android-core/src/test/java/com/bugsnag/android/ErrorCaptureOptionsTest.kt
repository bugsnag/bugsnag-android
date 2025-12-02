package com.bugsnag.android

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
        val fields = ErrorCaptureOptions.CAPTURE_STACKTRACE or ErrorCaptureOptions.CAPTURE_USER
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
        val fields = ErrorCaptureOptions.CAPTURE_BREADCRUMBS or ErrorCaptureOptions.CAPTURE_FEATURE_FLAGS
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
}
