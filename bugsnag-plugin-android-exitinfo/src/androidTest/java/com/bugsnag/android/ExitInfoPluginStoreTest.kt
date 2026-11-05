package com.bugsnag.android

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        // legacy files have no timestamp, so historical exit reasons must not be filtered out
        assertEquals(0L, store.previousState?.timestamp)
    }

    @Test
    fun testParsesJsonFileWithoutTimestamp() {
        // format written by v6.16.x and earlier: pid + exitInfoKeys, no timestamp
        storeFile.writeText(
            """{"pid":4321,"exitInfoKeys":[{"pid":"1234","timestamp":"1000"}]}"""
        )
        val store = ExitInfoPluginStore(storeFile, mock())

        val previousState = store.previousState
        assertNotNull(previousState)
        assertEquals(4321, previousState?.pid)
        assertEquals(0L, previousState?.timestamp)
        assertEquals(setOf(ExitInfoKey(1234, 1000L)), previousState?.processedExitInfoKeys)
        assertTrue(store.currentState.timestamp > 0L)
    }

    @Test
    fun testParsesJsonFileWithTimestamp() {
        storeFile.writeText(
            """{"pid":4321,"timestamp":"5000","exitInfoKeys":[]}"""
        )
        val store = ExitInfoPluginStore(storeFile, mock())

        assertEquals(4321, store.previousState?.pid)
        assertEquals(5000L, store.previousState?.timestamp)
    }

    @Test
    fun testLegacyFileIsRewrittenInCurrentFormat() {
        storeFile.writeText("""{"pid":4321,"exitInfoKeys":[]}""")
        val store = ExitInfoPluginStore(storeFile, mock())
        store.addExitInfoKey(ExitInfoKey(4321, 2000L))

        val restoredStore = ExitInfoPluginStore(storeFile, mock())
        val previousState = restoredStore.previousState
        assertNotNull(previousState)
        assertEquals(android.os.Process.myPid(), previousState?.pid)
        assertTrue((previousState?.timestamp ?: 0L) > 0L)
        assertEquals(setOf(ExitInfoKey(4321, 2000L)), previousState?.processedExitInfoKeys)
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
