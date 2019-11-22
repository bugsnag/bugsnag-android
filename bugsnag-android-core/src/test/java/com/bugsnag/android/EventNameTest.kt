package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import java.lang.Thread

class EventNameTest {

    private lateinit var event: Event

    @Before
    fun setUp() {
        val config = BugsnagTestUtils.generateImmutableConfig()
        val exception = RuntimeException("Example message", RuntimeException("Another"))
        event = Event.Builder(config, exception, null, Thread.currentThread(), false,
            Metadata()
        ).build()
    }

    @Test
    fun exceptionTypeConverted() {
        assertTrue(event.exception is BugsnagException)
        assertTrue(event.exceptions.exception is BugsnagException)
        assertTrue(event.exceptions.exception.cause is RuntimeException)
    }

    @Test
    fun defaultExceptionName() {
        assertEquals("java.lang.RuntimeException", event.exceptionName)
    }

    @Test
    fun defaultExceptionMessage() {
        assertEquals("Example message", event.exceptionMessage)
    }

    @Test
    fun overrideExceptionName() {
        event.exceptionName = "Foo"
        assertEquals("Foo", event.exceptionName)
    }

    @Test
    fun overrideExceptionMessage() {
        event.exceptionMessage = "Some custom message"
        assertEquals("Some custom message", event.exceptionMessage)
    }

}
