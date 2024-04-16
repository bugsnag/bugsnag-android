package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class SessionTest {

    @Mock
    lateinit var device: DeviceWithState

    @Mock
    lateinit var app: AppWithState

    private val apiKey = "BUGSNAG_API_KEY"

    private var session = Session("123", Date(0), User(), true, Notifier(), NoopLogger, apiKey)

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
    fun overrideApiKey() {
        assertEquals("BUGSNAG_API_KEY", session.apiKey)
        session.apiKey = "foo"
        assertEquals("foo", session.apiKey)
    }

    @Test
    fun defaultApiKey() {
        val file = File("_my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu1504255147933_v3.json")
        session = Session(
            "123", Date(0), User(), true, Notifier(), NoopLogger,
            SessionFilenameInfo.findApiKeyInFilename(file, "Default apikey")
        )
        assertEquals("Default apikey", session.apiKey)
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

    @Test
    fun overrideApp() {
        assertNull(session.app)
        session.app = app
        assertEquals(app, session.app)
    }

    @Test
    fun overrideDevice() {
        assertNull(session.device)
        session.device = device
        assertEquals(device, session.device)
    }

    @Test
    fun isLegacyPayload() {
        assertTrue(session.isLegacyPayload)
        val file = File("150450000000053a27e4e-967c-4e5c-91be-2e86f2eb7cdc.json")
        assertTrue(Session(file, Notifier(), NoopLogger, apiKey).isLegacyPayload)
        assertFalse(
            Session(
                File("150450000000053a27e4e-967c-4e5c-91be-2e86f2eb7cdc_v2.json"),
                Notifier(),
                NoopLogger,
                apiKey
            ).isLegacyPayload
        )
    }

    @Test
    fun testCloneNotifier() {
        val original = Notifier()
        val dep = Notifier("bugsnag-cobol")
        original.dependencies = listOf(dep)
        val payload = Session(null, original, NoopLogger, apiKey)
        val copy = payload.notifier
        assertNotSame(original, copy)
        assertNotSame(original.dependencies, copy.dependencies)
        assertEquals(original.dependencies, copy.dependencies)
        assertEquals(original.name, copy.name)
        assertEquals(original.url, copy.url)
        assertEquals(original.version, copy.version)
    }

    private fun validateSessionCopied(copy: Session) {
        with(session) {
            assertEquals(id, copy.id)
            assertEquals(startedAt, copy.startedAt)
            assertEquals(getUser(), copy.getUser()) // make a shallow copy
            assertEquals(isAutoCaptured, copy.isAutoCaptured)
            assertEquals(markTracked(), copy.markTracked())
            assertEquals(markResumed(), copy.markResumed())
            assertEquals(isPaused, copy.isPaused)
            assertEquals(markPaused(), copy.markPaused())
            assertEquals(session.unhandledCount, copy.unhandledCount)
            assertEquals(session.handledCount, copy.handledCount)
        }
    }
}
