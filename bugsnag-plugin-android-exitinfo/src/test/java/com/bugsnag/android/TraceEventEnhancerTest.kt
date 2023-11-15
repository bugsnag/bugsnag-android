package com.bugsnag.android

import android.app.ApplicationExitInfo
import com.bugsnag.android.internal.ImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import com.bugsnag.android.Thread as BugsnagThread

class TraceEventEnhancerTest {
    private val mockLogger = mock(Logger::class.java)
    private val eventEnhancer = TraceEventEnhancer(mockLogger, emptySet())

    @Test
    fun testEventEnhancer() {
        val event = Event(
            RuntimeException("error"),
            mock(ImmutableConfig::class.java),
            SeverityReason.newInstance(SeverityReason.REASON_ANR),
            mockLogger
        )

        // populate an "existing" thread to be replaced
        event.threads.add(
            BugsnagThread(
                "98765",
                "main",
                ErrorType.ANDROID,
                true,
                BugsnagThread.State.RUNNABLE,
                mockLogger
            )
        )

        val exitInfo = mock(ApplicationExitInfo::class.java)
        `when`(exitInfo.traceInputStream).thenReturn(this::class.java.getResourceAsStream("/emulator-exit-anr-trace"))

        eventEnhancer(event, exitInfo)

        val mainThread = event.threads.find { it.id == "1" }!!
        assertEquals(18, mainThread.stacktrace.size)
        assertEquals("java.lang.Thread.sleep", mainThread.stacktrace.first().method)
        assertEquals("com.android.internal.os.ZygoteInit.main", mainThread.stacktrace.last().method)
    }
}
