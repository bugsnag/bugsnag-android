package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateEvent
import com.bugsnag.android.FileStore.Delegate
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.convertToImmutableConfig
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch

class EmptyEventCallbackTest {

    private lateinit var storageDir: File
    private lateinit var errorDir: File

    @Before
    fun setUp() {
        storageDir = Files.createTempDirectory("tmp").toFile()
        storageDir.deleteRecursively()
        errorDir = File(storageDir, "bugsnag/errors")
    }

    @After
    fun tearDown() {
        storageDir.deleteRecursively()
    }

    @Test
    fun emptyQueuedEventTriggerEventStoreEmptyCallback() {
        val config = generateConfiguration().apply {
            maxPersistedEvents = 0
            persistenceDirectory = storageDir
        }
        val eventStore = createEventStore(convertToImmutableConfig(config))

        val event = generateEvent()
        eventStore.write(event)
        val callbackLatch = CountDownLatch(1)
        eventStore.onEventStoreEmptyCallback = { callbackLatch.countDown() }
        eventStore.flushAsync()
        callbackLatch.await()
    }

    @Test
    fun multipleQueueEvents() {
        val config = generateConfiguration().apply {
            maxPersistedEvents = 1
            persistenceDirectory = storageDir
        }
        val eventStore = createEventStore(convertToImmutableConfig(config))

        val event = generateEvent()
        eventStore.write(event)
        val callbackLatch = CountDownLatch(0)
        eventStore.onEventStoreEmptyCallback = { callbackLatch.countDown() }
        eventStore.flushAsync()
        callbackLatch.await()
    }

    private fun createEventStore(config: ImmutableConfig): EventStore {
        return EventStore(
            config,
            NoopLogger,
            Notifier(),
            BackgroundTaskService(),
            object : Delegate {
                override fun onErrorIOFailure(
                    exception: Exception?,
                    errorFile: File?,
                    context: String?
                ) {
                }
            },
            CallbackState()
        )
    }
}
