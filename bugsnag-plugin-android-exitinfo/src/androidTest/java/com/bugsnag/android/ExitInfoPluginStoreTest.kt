package com.bugsnag.android

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.File

class ExitInfoPluginStoreTest {
    private lateinit var storeFile: File

    @Before
    fun setUp() {
        storeFile = File.createTempFile("bugsnag-exit-reasons", null)
    }

    @After
    fun tearDown() {
        storeFile.delete()
    }

    @Test
    fun testStoreInitialization() {
        val store = ExitInfoPluginStore(storeFile, mock())

        // Verify that the store initializes correctly
        assert(store.previousState == null)
        assert(store.currentState.processedExitInfoKeys.isEmpty())
        assert(store.currentState.pid == android.os.Process.myPid())
        assert(store.currentState.timestamp > 0)
    }

    @Test
    fun testParsesLegacyFile() {
        storeFile.writeText("1234567890")
        val store = ExitInfoPluginStore(storeFile, mock())

        assertEquals(1234567890, store.previousState?.pid)
        assertEquals(true, store.previousState?.processedExitInfoKeys?.isEmpty())
    }

    @Test
    fun testPersistsExitInfoKeys() {
        val store = ExitInfoPluginStore(storeFile, mock())
        store.addExitInfoKey(ExitInfoKey(1234, 1234567890L))
        store.addExitInfoKey(ExitInfoKey(5678, 1234567891L))

        val restoredStore = ExitInfoPluginStore(storeFile, mock())
        assertEquals(2, restoredStore.previousState?.processedExitInfoKeys?.size)
        val previousState = restoredStore.previousState
        assertNotNull(previousState)
        assertEquals(
            true,
            previousState?.processedExitInfoKeys?.contains(ExitInfoKey(1234, 1234567890L))
        )
        assertEquals(
            true,
            previousState?.processedExitInfoKeys?.contains(ExitInfoKey(5678, 1234567891L))
        )
    }
}
