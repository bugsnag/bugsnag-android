package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class RemoteConfigStoreTest {
    private lateinit var storeFile: File
    private val appVersionCode = 1

    private val remoteConfig = RemoteConfig(
        "tag123",
        DateUtils.fromIso8601("2024-01-15T10:30:45.123Z"),
        listOf(DiscardRule.All())
    )

    @Before
    fun setUp() {
        storeFile = File(System.getProperty("java.io.tmpdir"), this::class.simpleName!!)
    }

    @After
    fun tearDown() {
        storeFile.delete()
    }

    @Test
    fun testStoreInitialization() {
        val store = RemoteConfigStore(storeFile, appVersionCode)
        store.update(null)
        val restoredConfig = store.load()
        assertNull(restoredConfig)
    }

    @Test
    fun testStore() {
        val store = RemoteConfigStore(storeFile, appVersionCode)
        store.update(remoteConfig)
        val restoredConfig = store.load()
        assertNotNull(restoredConfig)
        assertEquals("tag123", restoredConfig?.configurationTag)
        assertEquals(
            DateUtils.fromIso8601("2024-01-15T10:30:45.123Z"),
            restoredConfig?.configurationExpiry
        )
        assertEquals(1, restoredConfig?.discardRules?.size)
    }
}
