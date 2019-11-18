package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ErrorNameTest {

    private lateinit var error: Error

    @Before
    fun setUp() {
        val config = BugsnagTestUtils.generateImmutableConfig()
        val exception = RuntimeException("Example message", RuntimeException("Another"))
        error = Error.Builder(config, exception, null, Thread.currentThread(), false, MetaData()).build()
    }

    @Test
    fun exceptionTypeConverted() {
        assertTrue(error.exception is BugsnagException)
        assertTrue(error.exceptions.exception is BugsnagException)
        assertTrue(error.exceptions.exception.cause is RuntimeException)
    }

    @Test
    fun defaultExceptionName() {
        assertEquals("java.lang.RuntimeException", error.exceptionName)
    }

    @Test
    fun defaultExceptionMessage() {
        assertEquals("Example message", error.exceptionMessage)
    }

    @Test
    fun overrideExceptionName() {
        error.exceptionName = "Foo"
        assertEquals("Foo", error.exceptionName)
    }

    @Test
    fun overrideExceptionMessage() {
        error.exceptionMessage = "Some custom message"
        assertEquals("Some custom message", error.exceptionMessage)
    }

}
