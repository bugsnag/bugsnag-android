package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class SessionFilenameTest {
    lateinit var session: Session
    private val config = BugsnagTestUtils.generateImmutableConfig()

    @Before
    fun setUp() {
        session = BugsnagTestUtils.generateSession()
        session.apiKey = "TEST APIKEY"
    }

    @Test
    fun sessionFileName() {
        val fileName = SessionFilenameInfo(
            "TEST APIKEY",
            1504255147933,
            "my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu"
        ).encode()
        assertEquals("TEST APIKEY_my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu1504255147933_v2.json", fileName)
    }

    @Test
    fun getSessionDetailsFromFileName() {
        val fileName = SessionFilenameInfo(
            "TEST APIKEY",
            1504255147933,
            "my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu"
        ).encode()

        val file = File(fileName)
        val sessionInfo = SessionFilenameInfo.fromFile(file, config)
        assertEquals("TEST APIKEY", sessionInfo.apiKey)
        assertEquals(1504255147933, sessionInfo.timestamp)
        assertEquals("my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu", sessionInfo.uuid)
    }

    @Test
    fun getSessionApiKeyFromFileNameWithNoApiKey() {
        val fileName = SessionFilenameInfo(
            "",
            1504255147933,
            "my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu"
        ).encode()

        val file = File(fileName)
        val sessionInfo = SessionFilenameInfo.fromFile(file, config)
        assertEquals(config.apiKey, sessionInfo.apiKey)
    }
}
