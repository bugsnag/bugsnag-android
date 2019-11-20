package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig

import org.junit.Assert.assertEquals

import org.junit.Test

class BeforeNotifyTest {

    private val config = generateImmutableConfig()

    @Test
    fun testRunModifiesError() {
        val context = "new-context"

        val beforeNotify = BeforeNotify {
            it.context = context
            false
        }

        val event = Event.Builder(config, RuntimeException("Test"), null,
            Thread.currentThread(), false, MetaData()).build()
        beforeNotify.run(event)
        assertEquals(context, event.context)
    }
}
