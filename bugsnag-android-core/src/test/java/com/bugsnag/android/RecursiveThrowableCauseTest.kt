package com.bugsnag.android

import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Test that delivering errors with a self-referencing cause chain does not break the reporter.
 */
class RecursiveThrowableCauseTest {
    @Test(timeout = 100L)
    fun testStrictModeHandler() {
        val handler = StrictModeHandler()
        val throwableChain = createRecursiveThrowableChain()
        assertFalse(handler.isStrictModeThrowable(throwableChain))
    }

    @Test(timeout = 100L)
    fun testCreateEvent() {
        Error.createError(createRecursiveThrowableChain(), emptyList(), NoopLogger)
    }

    private fun createRecursiveThrowableChain(): Throwable {
        val t1 = Throwable()
        val t2 = Throwable()
        val t3 = Throwable()

        t1.initCause(t2)
        t2.initCause(t3)
        t3.initCause(t1)

        return t1
    }
}
