package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig

import org.junit.Assert.assertEquals

import org.junit.Test

import java.lang.Thread

class BeforeNotifyTest {

    private val config = generateImmutableConfig()

    @Test
    fun testRunModifiesError() {
        val context = "new-context"

        val beforeNotify = BeforeNotify {
            it.context = context
            false
        }

        val error = Error.Builder(config, RuntimeException("Test"), null,
            Thread.currentThread(), false, MetaData()).build()
        beforeNotify.run(error)
        assertEquals(context, error.context)
    }
}
