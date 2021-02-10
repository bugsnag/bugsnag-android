package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class OnErrorCallbackTest {

    private val config = generateImmutableConfig()

    @Test
    fun testRunModifiesError() {
        val context = "new-context"

        val onError = OnErrorCallback {
            it.context = context
            false
        }

        val handledState = SeverityReason.newInstance(
            SeverityReason.REASON_HANDLED_EXCEPTION
        )
        val error = Event(RuntimeException("Test"), config, handledState, NoopLogger)
        onError.onError(error)
        assertEquals(context, error.context)
    }
}
