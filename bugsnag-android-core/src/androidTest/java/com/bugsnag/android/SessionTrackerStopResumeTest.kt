package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateClient
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateSessionStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionTrackerStopResumeTest {

    private val configuration = generateConfiguration().also {
        it.autoCaptureSessions = false
    }
    private val sessionStore = generateSessionStore()
    private lateinit var tracker: SessionTracker

    private var client: Client? = null

    @Before
    fun setUp() {
        client = generateClient()
        tracker = SessionTracker(BugsnagTestUtils.generateImmutableConfig(),
            configuration, client, sessionStore)
    }

    @After
    fun tearDown() {
        client?.close()
    }

    /**
     * Verifies that a session can be resumed after it is stopped
     */
    @Test
    fun resumeFromStoppedSession() {
        tracker.startSession(false)
        val originalSession = tracker.currentSession
        assertNotNull(originalSession)

        tracker.stopSession()
        assertNull(tracker.currentSession)

        assertTrue(tracker.resumeSession())
        assertEquals(originalSession, tracker.currentSession)
    }

    /**
     * Verifies that a new session is started when calling [SessionTracker.resumeSession],
     * if there is no stopped session
     */
    @Test
    fun resumeWithNoStoppedSession() {
        assertNull(tracker.currentSession)
        assertFalse(tracker.resumeSession())
        assertNotNull(tracker.currentSession)
    }

    /**
     * Verifies that a new session can be created after the previous one is stopped
     */
    @Test
    fun startNewAfterStoppedSession() {
        tracker.startSession(false)
        val originalSession = tracker.currentSession

        tracker.stopSession()
        tracker.startSession(false)
        assertNotEquals(originalSession, tracker.currentSession)
    }

    /**
     * Verifies that calling [SessionTracker.resumeSession] multiple times only starts one session
     */
    @Test
    fun multipleResumesHaveNoEffect() {
        tracker.startSession(false)
        val original = tracker.currentSession
        tracker.stopSession()

        assertTrue(tracker.resumeSession())
        assertEquals(original, tracker.currentSession)

        assertFalse(tracker.resumeSession())
        assertEquals(original, tracker.currentSession)
    }

    /**
     * Verifies that calling [SessionTracker.stopSession] multiple times only stops one session
     */
    @Test
    fun multipleStopsHaveNoEffect() {
        tracker.startSession(false)
        assertNotNull(tracker.currentSession)

        tracker.stopSession()
        assertNull(tracker.currentSession)

        tracker.stopSession()
        assertNull(tracker.currentSession)
    }

    /**
     * Verifies that if a handled or unhandled error occurs when a session is stopped, the
     * error count is not updated
     */
    @Test
    fun stoppedSessionDoesNotIncrement() {
        tracker.startSession(false)
        tracker.incrementHandledAndCopy()
        tracker.incrementUnhandledAndCopy()
        assertEquals(1, tracker.currentSession?.handledCount)
        assertEquals(1, tracker.currentSession?.unhandledCount)

        tracker.stopSession()
        tracker.incrementHandledAndCopy()
        tracker.incrementUnhandledAndCopy()
        tracker.resumeSession()
        assertEquals(1, tracker.currentSession?.handledCount)
        assertEquals(1, tracker.currentSession?.unhandledCount)

        tracker.incrementHandledAndCopy()
        tracker.incrementUnhandledAndCopy()
        assertEquals(2, tracker.currentSession?.handledCount)
        assertEquals(2, tracker.currentSession?.unhandledCount)
    }
}
