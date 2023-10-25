package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.Date

class SessionPayloadTest {
    lateinit var session: Session

    @Before
    fun setUp() {
        session = BugsnagTestUtils.generateSession()
        session.app = BugsnagTestUtils.generateApp()
        session.device = BugsnagTestUtils.generateDevice()
    }

    /**
     * Serialises sessions from a file instead
     */

    @Test
    fun testSessionV2PayloadFromFile() {
        // write file to disk

        val v2File = File.createTempFile(
            "150450000000053a27e4e-967c-4e5c-91be-2e86f2eb7cdc",
            "_v2.json"
        )
        val fos = FileOutputStream(v2File.absolutePath)
        val out: Writer = BufferedWriter(OutputStreamWriter(fos, "UTF-8"))
        val stream = JsonStream(out)
        session.toStream(stream)
        out.flush()
        val v2Payload = Session(v2File, Notifier(), NoopLogger, "_my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu")
        val v2Obj = BugsnagTestUtils.streamableToJson(v2Payload)
        assertNotNull(v2Obj.getJSONObject("app"))
        assertNotNull(v2Obj.getJSONObject("device"))
        assertNotNull(v2Obj.getJSONObject("notifier"))
        val sessions = v2Obj.getJSONArray("sessions")
        assertNotNull(sessions)
        assertEquals(1, sessions.length().toLong())
        val session = sessions.getJSONObject(0)
        assertEquals("test", session.getString("id"))
        assertFalse(session.has("notifier"))
    }

    @Test
    fun testSessionV3PayloadFromFile() {
        // write file to disk

        val v3File = File.createTempFile(
            "150450000000053a27e4e-967c-4e5c-91be-2e86f2eb7cdc",
            "_v3.json"
        )
        val fos = FileOutputStream(v3File.absolutePath)
        val out: Writer = BufferedWriter(OutputStreamWriter(fos, "UTF-8"))
        val stream = JsonStream(out)
        session.toStream(stream)
        out.flush()
        val v3Payload = Session(v3File, Notifier(), NoopLogger, "_my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu")
        val v3Object = BugsnagTestUtils.streamableToJson(v3Payload)
        assertNotNull(v3Object)
        assertNotNull(v3Object.getJSONObject("app"))
        assertNotNull(v3Object.getJSONObject("device"))
        assertNotNull(v3Object.getJSONObject("notifier"))
        val sessions = v3Object.getJSONArray("sessions")
        assertNotNull(sessions)
        assertEquals(1, sessions.length().toLong())
        val session = sessions.getJSONObject(0)
        assertEquals("test", session.getString("id"))
        assertFalse(session.has("notifier"))
    }

    @Test
    fun testAutoCapturedOverride() {
        session = Session(
            "id",
            Date(),
            null,
            false,
            Notifier(),
            NoopLogger,
            "TEST APIKEY"
        )
        assertFalse(session.isAutoCaptured)
        session.isAutoCaptured = true
        assertTrue(session.isAutoCaptured)
        val obj = BugsnagTestUtils.streamableToJson(session)
        val sessionNode = obj.getJSONArray("sessions").getJSONObject(0)
        assertFalse(sessionNode.has("user"))
    }
}
