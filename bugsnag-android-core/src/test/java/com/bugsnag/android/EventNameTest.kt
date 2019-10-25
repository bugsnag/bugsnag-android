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
        event = EventGenerator.Builder(config, exception, null, Thread.currentThread(), false, MetaData()).build()
    }

    @Test
    fun defaultExceptionName() {
        assertEquals("java.lang.RuntimeException", event.errors[0].errorClass)
    }

    @Test
    fun defaultExceptionMessage() {
        assertEquals("Example message", event.errors[0].errorMessage)
    }

    @Test
    fun overrideExceptionName() {
        event.errors[0].errorClass = "Foo"
        assertEquals("Foo", event.errors[0].errorClass)
    }

    @Test
    fun overrideExceptionMessage() {
        event.errors[0].errorMessage = "Some custom message"
        assertEquals("Some custom message", event.errors[0].errorMessage)
    }

}
