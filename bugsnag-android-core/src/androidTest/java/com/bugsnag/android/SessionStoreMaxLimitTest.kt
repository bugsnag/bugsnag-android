package com.bugsnag.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import com.bugsnag.android.BugsnagTestUtils.generateSession
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Verifies that the maxPersistedSessions configuration option is respected when writing sessions.
 */
class SessionStoreMaxLimitTest {

    private lateinit var sessionStore: SessionStore
    private lateinit var storageDir: File

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        storageDir = File(ctx.cacheDir, "bugsnag-sessions")
        storageDir.deleteRecursively()
    }

    @After
    fun tearDown() {
        storageDir.deleteRecursively()
    }

    @Test
    fun testDefaultLimit() {
        val config = generateImmutableConfig()
        sessionStore = createSessionStore(config)

        val session = generateSession()
        repeat(40) {
            sessionStore.write(session)
        }
        val files = storageDir.list()
        assertEquals(32, files.size)
    }

    @Test
    fun testDifferentLimit() {
        val config = generateConfiguration().apply {
            maxPersistedSessions = 5
        }
        sessionStore = createSessionStore(convertToImmutableConfig(config))

        val session = generateSession()
        repeat(7) {
            sessionStore.write(session)
        }
        val files = storageDir.list()
        assertEquals(5, files.size)
    }

    @Test
    fun testZeroLimit() {
        val config = generateConfiguration().apply {
            maxPersistedSessions = 0
        }
        sessionStore = createSessionStore(convertToImmutableConfig(config))

        val session = generateSession()
        sessionStore.write(session)
        val files = storageDir.list()
        assertEquals(0, files.size)
    }

    private fun createSessionStore(config: ImmutableConfig): SessionStore {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        return SessionStore(
            ctx,
            config,
            NoopLogger,
            FileStore.Delegate { _, _, _ -> }
        )
    }
}
