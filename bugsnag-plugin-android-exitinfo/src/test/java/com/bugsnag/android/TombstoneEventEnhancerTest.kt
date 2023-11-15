package com.bugsnag.android

import android.app.ApplicationExitInfo
import com.bugsnag.android.internal.ImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import com.bugsnag.android.Thread as BugsnagThread

internal class TombstoneEventEnhancerTest {

    private val logger = mock(Logger::class.java)

    private val tombstoneEventEnhancer = TombstoneEventEnhancer(
        logger = logger,
        listOpenFds = true,
        includeLogcat = true
    )

    @Test
    fun testTombstoneEnhancer() {
        val file = this.javaClass.getResourceAsStream("/tombstone_01.pb")
        val event = Event(
            RuntimeException("error"),
            mock(ImmutableConfig::class.java),
            SeverityReason.newInstance(SeverityReason.REASON_SIGNAL),
            logger
        )
        val oldThread = BugsnagThread(
            "30639",
            "test",
            ErrorType.UNKNOWN,
            true,
            BugsnagThread.State.TERMINATED,
            logger
        )

        event.threads.add(oldThread)

        val exitInfo = mock(ApplicationExitInfo::class.java)
        `when`(exitInfo.traceInputStream).thenReturn(file)

        val testOldThread = event.threads.find { it.id == "30639" }!!
        assertEquals(0, testOldThread.stacktrace.size)
        assertEquals("test", testOldThread.name)

        tombstoneEventEnhancer(event, exitInfo)

        val testThread = event.threads.find { it.id == "30639" }!!
        assertEquals("POSIX timer 0", testOldThread.name)
        assertEquals(4, testThread.stacktrace.size)
        assertEquals(667096L, testThread.stacktrace.first().lineNumber)
        assertEquals("__rt_sigtimedwait", testThread.stacktrace.first().method)
        assertEquals("__start_thread", testThread.stacktrace.last().method)

        val firstFd = event.getMetadata("Open FileDescriptors")!!["0"] as Map<*, *>
        assertEquals("/dev/null", firstFd["path"])
        assertNull(firstFd["owner"])

        val logMetadata = event.getMetadata("Log Messages")
        val logMessage = logMetadata!!["Log Messages"]
        assertNotNull(logMessage)
    }
}
