package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ErrorTest {

    @Test
    fun createError() {
        val err = Error.createError(RuntimeException("Whoops"), setOf(), NoopLogger)
        assertEquals(1, err.size)
        assertEquals("Whoops", err[0].errorMessage)
        assertFalse(err[0].stacktrace.isEmpty())
    }

    @Test
    fun createNestedError() {
        val err = Error.createError(IllegalStateException("Some err", RuntimeException("Whoops")), setOf(), NoopLogger)
        assertEquals(2, err.size)
        assertEquals("Some err", err[0].errorMessage)
        assertFalse(err[0].stacktrace.isEmpty())

        assertEquals("Whoops", err[1].errorMessage)
        assertFalse(err[1].stacktrace.isEmpty())
    }
}
