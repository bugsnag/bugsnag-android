package com.bugsnag.android

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

class NotifyTest {
    private var config: Configuration? = null
    private var client: Client? = null

    /**
     * Generates a configuration and clears sharedPrefs values to begin the test with a clean slate
     */
    @Before
    fun setUp() {
        config = BugsnagTestUtils.generateConfiguration()
        client = BugsnagTestUtils.generateClient(config)
    }

    @After
    fun tearDown() {
        client?.close()
        client = null
    }

    @Test
    fun testCrashAndNotify() {
        client!!.notifyUnhandledException(
            RuntimeException("Fake crash"),
            Metadata(),
            SeverityReason.REASON_UNHANDLED_EXCEPTION,
            null
        )

        // this would throw an Exception if the BackgroundTaskService was shutdown
        client!!.notifyUnhandledException(
            IOException("The app should have crashed & shut down by now"),
            Metadata(),
            SeverityReason.REASON_UNHANDLED_EXCEPTION,
            null
        )
    }
}
