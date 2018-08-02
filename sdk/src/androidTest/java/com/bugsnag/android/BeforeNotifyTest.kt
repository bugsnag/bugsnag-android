package com.bugsnag.android

import org.junit.Assert.assertEquals

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class BeforeNotifyTest {

    private val config = Configuration("api-key")

    @Test
    fun testRunModifiesError() {
        val context = "new-context"

        val beforeNotify = BeforeNotify {
            it.context = context
            false
        }

        val error = Error.Builder(config, RuntimeException("Test"), null).build()
        beforeNotify.run(error)
        assertEquals(context, error.context)
    }
}
