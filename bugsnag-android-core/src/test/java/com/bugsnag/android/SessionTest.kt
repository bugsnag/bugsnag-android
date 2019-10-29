package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Date

class SessionTest {

    private val session = Session("123", Date(), User(null, null, null), true)

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

    private fun validateSessionCopied(copy: Session) {
        with(session) {
            assertEquals(id, copy.id)
            assertEquals(startedAt, copy.startedAt)
            assertEquals(user, copy.user) // make a shallow copy
            assertEquals(isAutoCaptured, copy.isAutoCaptured)
            assertEquals(isTracked.get(), copy.isTracked.get())
            assertEquals(session.unhandledCount, copy.unhandledCount)
            assertEquals(session.handledCount, copy.handledCount)
        }
    }
}
