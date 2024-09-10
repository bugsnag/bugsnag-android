package com.bugsnag.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bugsnag.android.internal.ImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        exitInfoPluginStore = ExitInfoPluginStore(immutableConfig)
    }

    /**
     * Null should be returned for non-existent files
     */
    @Test
    fun readNonExistentFile() {
        val expectedResult = null to emptySet<ExitInfoKey>()
        assertEquals(expectedResult, exitInfoPluginStore.load())
    }

    /**
     * Null should be returned for empty files
     */
    @Test
    fun readEmptyFile() {
        file.createNewFile()
        val expectedResult = null to emptySet<ExitInfoKey>()
        assertEquals(expectedResult, exitInfoPluginStore.load())
    }

    /**
     * Null should be returned for invalid file contents
     */
    @Test
    fun readInvalidFileContents() {
        file.writeText("{\"hamster\": 2}")
        val expectedResult = null to emptySet<ExitInfoKey>()
        assertEquals(expectedResult, exitInfoPluginStore.load())
    }

    /**
     * A valid PID should be returned for legacy files, which contained only the PID of the previous run
     */
    @Test
    fun readLegacyFileContents() {
        file.writeText("12345")
        exitInfoPluginStore = ExitInfoPluginStore(immutableConfig)
        val info = requireNotNull(exitInfoPluginStore.load())
        assertEquals(12345, info.first)
    }

    @Test
    fun writableFileWithEmptyExitInfo() {
        exitInfoPluginStore.persist(12345, emptySet())
        val firstPid = exitInfoPluginStore.load().first
        assertNull(firstPid)
        exitInfoPluginStore = ExitInfoPluginStore(immutableConfig)
        val (storedPid, storageExitInfoKeys) = exitInfoPluginStore.load()
        assertEquals(12345, storedPid)
        assertEquals(emptySet<ExitInfoKey>(), storageExitInfoKeys)
    }

    @Test
    fun writableFile() {
        val expectedPid = 12345
        val expectedExitInfoKeys = setOf(ExitInfoKey(111, 100L))
        exitInfoPluginStore.persist(expectedPid, expectedExitInfoKeys)

        val (storedPid, storageExitInfoKeys) = exitInfoPluginStore.load()
        assertNull(storedPid)
        assertEquals(emptySet<ExitInfoKey>(), storageExitInfoKeys)

        exitInfoPluginStore = ExitInfoPluginStore(immutableConfig)
        val (storedPid2, storageExitInfoKeys2) = exitInfoPluginStore.load()
        assertEquals(expectedPid, storedPid2)
        assertEquals(expectedExitInfoKeys, storageExitInfoKeys2)
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
