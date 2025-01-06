package com.bugsnag.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bugsnag.android.internal.ImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

internal class BugsnagExitInfoPluginStoreTest {

    private lateinit var file: File
    private lateinit var exitInfoPluginStore: ExitInfoPluginStore
    private lateinit var immutableConfig: ImmutableConfig

    @Before
    fun setUp() {
        immutableConfig = TestHooks.convertToImmutableConfig(
            generateConfiguration().apply {
                persistenceDirectory = ApplicationProvider.getApplicationContext<Context>().cacheDir
            }
        )
        file = File(immutableConfig.persistenceDirectory.value, "bugsnag-exit-reasons")
        file.delete()
    }

    /**
     * Null should be returned for non-existent files
     */
    @Test
    fun readNonExistentFile() {
        exitInfoPluginStore = ExitInfoPluginStore(immutableConfig)
        assertEquals(0, exitInfoPluginStore.currentPid)
        assertEquals(0, exitInfoPluginStore.previousPid)
        assertEquals(emptySet<ExitInfoKey>(), exitInfoPluginStore.exitInfoKeys)
    }

    /**
     * Null should be returned for empty files
     */
    @Test
    fun readEmptyFile() {
        file.createNewFile()
        exitInfoPluginStore = ExitInfoPluginStore(immutableConfig)

        assertEquals(0, exitInfoPluginStore.currentPid)
        assertEquals(0, exitInfoPluginStore.previousPid)
        assertEquals(emptySet<ExitInfoKey>(), exitInfoPluginStore.exitInfoKeys)
    }

    /**
     * Null should be returned for invalid file contents
     */
    @Test
    fun readInvalidFileContents() {
        file.writeText("{\"hamster\": 2}")
        exitInfoPluginStore = ExitInfoPluginStore(immutableConfig)

        assertEquals(0, exitInfoPluginStore.currentPid)
        assertEquals(0, exitInfoPluginStore.previousPid)
        assertEquals(emptySet<ExitInfoKey>(), exitInfoPluginStore.exitInfoKeys)
    }

    /**
     * A valid PID should be returned for legacy files, which contained only the PID of the previous run
     */
    @Test
    fun readLegacyFileContents() {
        file.writeText("12345")
        exitInfoPluginStore = ExitInfoPluginStore(immutableConfig)
        assertEquals(12345, exitInfoPluginStore.previousPid)
        assertEquals(0, exitInfoPluginStore.currentPid)
    }

    @Test
    fun addExitInfoKey() {
        file.writeText("12345")
        exitInfoPluginStore = ExitInfoPluginStore(immutableConfig)
        exitInfoPluginStore.currentPid = 54321

        assertEquals(12345, exitInfoPluginStore.previousPid)

        val expectedExitInfoKey = ExitInfoKey(111, 100L)
        exitInfoPluginStore.addExitInfoKey(expectedExitInfoKey)

        // reload the ExitInfoPluginStore
        exitInfoPluginStore = ExitInfoPluginStore(immutableConfig)

        assertEquals(54321, exitInfoPluginStore.previousPid)
        assertEquals(0, exitInfoPluginStore.currentPid)
        assertEquals(setOf(expectedExitInfoKey), exitInfoPluginStore.exitInfoKeys)
    }

    private fun generateConfiguration(): Configuration {
        val configuration = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        configuration.delivery = generateDelivery()
        return configuration
    }

    private fun generateDelivery(): Delivery {
        return object : Delivery {
            override fun deliver(
                payload: EventPayload,
                deliveryParams: DeliveryParams
            ): DeliveryStatus {
                return DeliveryStatus.DELIVERED
            }

            override fun deliver(
                payload: Session,
                deliveryParams: DeliveryParams
            ): DeliveryStatus {
                return DeliveryStatus.DELIVERED
            }
        }
    }
}
