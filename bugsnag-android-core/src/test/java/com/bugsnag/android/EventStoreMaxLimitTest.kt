package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Verifies that the maxPersistedEvents configuration option is respected when writing events.
 */
class EventStoreMaxLimitTest {

    private lateinit var storageDir: File
    private lateinit var errorDir: File

    @Before
    fun setUp() {
        storageDir = Files.createTempDirectory("tmp").toFile()
        storageDir.deleteRecursively()
        errorDir = File(storageDir, "bugsnag-errors")
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
        val eventStore = createEventStore(convertToImmutableConfig(config))

        val event = generateEvent()
        repeat(40) {
            eventStore.write(event)
        }
        val files = requireNotNull(errorDir.list())
        assertEquals(32, files.size)
    }

    @Test
    fun testDifferentLimit() {
        val config = generateConfiguration().apply {
            maxPersistedEvents = 5
            persistenceDirectory = storageDir
        }
        val eventStore = createEventStore(convertToImmutableConfig(config))

        val event = generateEvent()
        repeat(7) {
            eventStore.write(event)
        }
        val files = requireNotNull(errorDir.list())
        assertEquals(5, files.size)
    }

    @Test
    fun testZeroLimit() {
        val config = generateConfiguration().apply {
            maxPersistedEvents = 0
            persistenceDirectory = storageDir
        }
        val eventStore = createEventStore(convertToImmutableConfig(config))

        val event = generateEvent()
        eventStore.write(event)
        val files = requireNotNull(errorDir.list())
        assertEquals(0, files.size)
    }

    private fun createEventStore(config: ImmutableConfig): EventStore {
        return EventStore(
            config,
            NoopLogger,
            Notifier(),
            BackgroundTaskService(),
            FileStore.Delegate { _, _, _ -> }
        )
    }
}
