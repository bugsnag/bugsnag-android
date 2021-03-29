package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateSession
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Verifies that the maxPersistedSessions configuration option is respected when writing sessions.
 */
class SessionStoreMaxLimitTest {

    private lateinit var storageDir: File
    private lateinit var sessionDir: File

    @Before
    fun setUp() {
        storageDir = Files.createTempDirectory("tmp").toFile()
        storageDir.deleteRecursively()
        sessionDir = File(storageDir, "bugsnag-sessions")
    }

    @After
    fun tearDown() {
        storageDir.deleteRecursively()
    }

    @Test
    fun testDefaultLimit() {
        val config = generateConfiguration().apply {
            persistenceDirectory = storageDir
        }
        val sessionStore = createSessionStore(convertToImmutableConfig(config))

        val session = generateSession()
        repeat(140) {
            sessionStore.write(session)
        }
        val files = requireNotNull(sessionDir.list())
        assertEquals(128, files.size)
    }

    @Test
    fun testDifferentLimit() {
        val config = generateConfiguration().apply {
            maxPersistedSessions = 5
            persistenceDirectory = storageDir
        }
        val sessionStore = createSessionStore(convertToImmutableConfig(config))

        val session = generateSession()
        repeat(7) {
            sessionStore.write(session)
        }
        val files = requireNotNull(sessionDir.list())
        assertEquals(5, files.size)
    }

    @Test
    fun testZeroLimit() {
        val config = generateConfiguration().apply {
            maxPersistedSessions = 0
            persistenceDirectory = storageDir
        }
        val sessionStore = createSessionStore(convertToImmutableConfig(config))

        val session = generateSession()
        sessionStore.write(session)
        val files = requireNotNull(sessionDir.list())
        assertEquals(0, files.size)
    }

    private fun createSessionStore(config: ImmutableConfig): SessionStore {
        return SessionStore(
            config,
            NoopLogger,
            FileStore.Delegate { _, _, _ -> }
        )
    }
}
