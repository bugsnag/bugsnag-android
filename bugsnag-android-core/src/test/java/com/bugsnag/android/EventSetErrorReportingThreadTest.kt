package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EventSetErrorReportingThreadTest {

    private lateinit var event: Event
    private lateinit var config: ImmutableConfig
    private val logger = NoopLogger

    @Before
    fun setUp() {
        config = BugsnagTestUtils.generateImmutableConfig()
        val severityReason = SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION)
        event = Event(RuntimeException("Test"), config, severityReason, logger)
    }

    @Test
    fun testSetErrorReportingThreadByReference() {
        // Add some threads to the event
        val thread1 = event.addThread("1", "Thread-1")
        val thread2 = event.addThread("2", "Thread-2")
        val thread3 = event.addThread("3", "Thread-3")

        // Initially, no thread should be marked as error reporting
        assertFalse(thread1.errorReportingThread)
        assertFalse(thread2.errorReportingThread)
        assertFalse(thread3.errorReportingThread)

        // Set thread2 as error reporting thread
        event.setErrorReportingThread(thread2)

        // Verify only thread2 is marked as error reporting
        assertFalse(thread1.errorReportingThread)
        assertTrue(thread2.errorReportingThread)
        assertFalse(thread3.errorReportingThread)

        // Change to thread3
        event.setErrorReportingThread(thread3)

        // Verify only thread3 is now marked as error reporting
        assertFalse(thread1.errorReportingThread)
        assertFalse(thread2.errorReportingThread)
        assertTrue(thread3.errorReportingThread)
    }

    @Test
    fun testSetErrorReportingThreadByReferenceNotInEvent() {
        // Add some threads to the event
        val thread1 = event.addThread("1", "Thread-1")
        val thread2 = event.addThread("2", "Thread-2")

        // Create a thread that's not in the event
        val externalThread = Thread(
            "999", "External-Thread", ErrorType.ANDROID,
            false, Thread.State.RUNNABLE, logger
        )

        // Set thread1 as error reporting initially
        event.setErrorReportingThread(thread1)
        assertTrue(thread1.errorReportingThread)
        assertFalse(thread2.errorReportingThread)

        // Try to set external thread as error reporting - should have no effect
        event.setErrorReportingThread(externalThread)

        // Verify thread1 is still marked as error reporting (no change)
        assertTrue(thread1.errorReportingThread)
        assertFalse(thread2.errorReportingThread)
        assertFalse(externalThread.errorReportingThread)
    }

    @Test
    fun testSetErrorReportingThreadById() {
        // Add some threads to the event
        val thread1 = event.addThread(1L, "Thread-1")
        val thread2 = event.addThread(2L, "Thread-2")
        val thread3 = event.addThread(3L, "Thread-3")

        // Initially, no thread should be marked as error reporting
        assertFalse(thread1.errorReportingThread)
        assertFalse(thread2.errorReportingThread)
        assertFalse(thread3.errorReportingThread)

        // Set thread with id 2 as error reporting thread
        event.setErrorReportingThread(2L)

        // Verify only thread2 is marked as error reporting
        assertFalse(thread1.errorReportingThread)
        assertTrue(thread2.errorReportingThread)
        assertFalse(thread3.errorReportingThread)

        // Change to thread with id 3
        event.setErrorReportingThread(3L)

        // Verify only thread3 is now marked as error reporting
        assertFalse(thread1.errorReportingThread)
        assertFalse(thread2.errorReportingThread)
        assertTrue(thread3.errorReportingThread)
    }

    @Test
    fun testSetErrorReportingThreadByIdNotFound() {
        // Add some threads to the event
        val thread1 = event.addThread(1L, "Thread-1")
        val thread2 = event.addThread(2L, "Thread-2")

        // Set thread1 as error reporting initially
        event.setErrorReportingThread(1L)
        assertTrue(thread1.errorReportingThread)
        assertFalse(thread2.errorReportingThread)

        // Try to set thread with non-existent id 999 as error reporting - should have no effect
        event.setErrorReportingThread(999L)

        // Verify thread1 is still marked as error reporting (no change)
        assertTrue(thread1.errorReportingThread)
        assertFalse(thread2.errorReportingThread)
    }

    @Test
    fun testSetErrorReportingThreadMultipleChanges() {
        // Add threads to the event
        val thread1 = event.addThread("100", "Thread-100")
        val thread2 = event.addThread("200", "Thread-200")
        val thread3 = event.addThread("300", "Thread-300")

        // Set each thread as error reporting in sequence and verify exclusivity
        event.setErrorReportingThread(thread1)
        assertTrue(thread1.errorReportingThread)
        assertFalse(thread2.errorReportingThread)
        assertFalse(thread3.errorReportingThread)

        event.setErrorReportingThread(200L) // Use ID this time
        assertFalse(thread1.errorReportingThread)
        assertTrue(thread2.errorReportingThread)
        assertFalse(thread3.errorReportingThread)

        event.setErrorReportingThread(thread3) // Back to reference
        assertFalse(thread1.errorReportingThread)
        assertFalse(thread2.errorReportingThread)
        assertTrue(thread3.errorReportingThread)
    }

    @Test
    fun testSetErrorReportingThreadWithNoAdditionalThreads() {
        // Event should already have the default thread(s) from creation
        val initialThreads = event.threads
        val hasErrorReportingThread = initialThreads.any { it.errorReportingThread }

        // Try to set error reporting thread by ID when no matching threads exist
        event.setErrorReportingThread(999L)

        // Verify the error reporting thread state is unchanged
        val afterThreads = event.threads
        assertEquals(initialThreads.size, afterThreads.size)
        assertEquals(hasErrorReportingThread, afterThreads.any { it.errorReportingThread })
    }
}
