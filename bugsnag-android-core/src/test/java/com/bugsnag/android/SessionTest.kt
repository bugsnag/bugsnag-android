package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Date

class SessionTest {

    private val session = Session("123", Date(0), User(), true)

    /**
     * Verifies that all the fields in session are copied into a new object correctly
     */
    @Test
    fun copySession() {
        val copy = Session.copySession(session)
        assertNotEquals(session, copy)
        validateSessionCopied(copy)
    }

    @Test
    fun handledIncrementCopiesSession() {
        val copy = session.incrementHandledAndCopy()
        assertNotEquals(session, copy)
        validateSessionCopied(copy)
    }

    @Test
    fun unhandledIncrementCopiesSession() {
        val copy = session.incrementUnhandledAndCopy()
        assertNotEquals(session, copy)
        validateSessionCopied(copy)
    }

    @Test
    fun overrideId() {
        assertEquals("123", session.id)
        session.id = "foo"
        assertEquals("foo", session.id)
    }

    @Test
    fun overrideStartedAt() {
        assertEquals(0, session.startedAt.time)
        session.startedAt = Date(5)
        assertEquals(5, session.startedAt.time)
    }

    @Test
    fun overrideUser() {
        assertNotNull(session.getUser())
        session.setUser("123", "joe@example.com", "Joey")
        assertEquals("123", session.getUser().id)
        assertEquals("joe@example.com", session.getUser().email)
        assertEquals("Joey", session.getUser().name)
    }

    private fun validateSessionCopied(copy: Session) {
        with(session) {
            assertEquals(id, copy.id)
            assertEquals(startedAt, copy.startedAt)
            assertEquals(getUser(), copy.getUser()) // make a shallow copy
            assertEquals(isAutoCaptured, copy.isAutoCaptured)
            assertEquals(isTracked.get(), copy.isTracked.get())
            assertEquals(session.unhandledCount, copy.unhandledCount)
            assertEquals(session.handledCount, copy.handledCount)
        }
    }
}
