package com.bugsnag.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateEvent
import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Verifies that the maxPersistedEvents configuration option is respected when writing events.
 */
class EventStoreMaxLimitTest {

    private lateinit var eventStore: EventStore
    private lateinit var storageDir: File

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        storageDir = File(ctx.cacheDir, "bugsnag-errors")
        storageDir.deleteRecursively()
    }

    @After
    fun tearDown() {
        storageDir.deleteRecursively()
    }

    @Test
    fun testDefaultLimit() {
        val config = generateImmutableConfig()
        eventStore = createEventStore(config)

        val event = generateEvent()
        repeat(40) {
            eventStore.write(event)
        }
        val files = storageDir.list()
        assertEquals(32, files.size)
    }

    @Test
    fun testDifferentLimit() {
        val config = generateConfiguration().apply {
            maxPersistedEvents = 5
        }
        eventStore = createEventStore(convertToImmutableConfig(config))

        val event = generateEvent()
        repeat(7) {
            eventStore.write(event)
        }
        val files = storageDir.list()
        assertEquals(5, files.size)
    }

    @Test
    fun testZeroLimit() {
        val config = generateConfiguration().apply {
            maxPersistedEvents = 0
        }
        eventStore = createEventStore(convertToImmutableConfig(config))

        val event = generateEvent()
        eventStore.write(event)
        val files = storageDir.list()
        assertEquals(0, files.size)
    }

    private fun createEventStore(config: ImmutableConfig): EventStore {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        return EventStore(
            config,
            ctx,
            NoopLogger,
            Notifier(),
            FileStore.Delegate { _, _, _ -> }
        )
    }
}
