package com.bugsnag.android

import org.junit.Assert.assertEquals

import androidx.test.filters.SmallTest
import com.bugsnag.android.BugsnagTestUtils.generateSessionTracker

import org.junit.Test

@SmallTest
class BeforeNotifyTest {

    private val config = BugsnagTestUtils.generateImmutableConfig()

    @Test
    fun testRunModifiesError() {
        val context = "new-context"

        val beforeNotify = BeforeNotify {
            it.context = context
            false
        }

        val error = Error.Builder(config, RuntimeException("Test"), generateSessionTracker(),
            Thread.currentThread(), false, MetaData()).build()
        beforeNotify.run(error)
        assertEquals(context, error.context)
    }
}
