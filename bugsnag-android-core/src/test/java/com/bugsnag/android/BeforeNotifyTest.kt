package com.bugsnag.android

import org.junit.Assert.assertEquals

import org.junit.Test

class BeforeNotifyTest {

    private val config = Configuration("api-key")

    @Test
    fun testRunModifiesError() {
        val context = "new-context"

        val beforeNotify = BeforeNotify {
            it.context = context
            false
        }

        val error = Error.Builder(config, RuntimeException("Test"), null,
            Thread.currentThread(), false).build()
        beforeNotify.run(error)
        assertEquals(context, error.context)
    }
}
