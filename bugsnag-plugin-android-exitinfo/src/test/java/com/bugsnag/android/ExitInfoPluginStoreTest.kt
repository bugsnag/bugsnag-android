package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.nio.file.Files

internal class ExitInfoPluginStoreTest {

    private lateinit var file: File
    private lateinit var exitInfoPluginStore: ExitInfoPluginStore
    private lateinit var storageDir: File

    private val immutableConfig = mock(ImmutableConfig::class.java)
    private val logger = mock(Logger::class.java)

    @Before
    fun setUp() {
        storageDir = Files.createTempDirectory("tmp").toFile()
        `when`(immutableConfig.persistenceDirectory).thenReturn(lazy { storageDir })
        `when`(immutableConfig.logger).thenReturn(logger)
        file = File(immutableConfig.persistenceDirectory.value, "bugsnag-exit-reasons")
        file.delete()
        exitInfoPluginStore = ExitInfoPluginStore(immutableConfig)
    }

    /**
     * Null should be returned for non-existent files
     */
    @Test
    fun readNonExistentFile() {
        assertNull(exitInfoPluginStore.load())
    }

    /**
     * Null should be returned for empty files
     */
    @Test
    fun readEmptyFile() {
        file.createNewFile()
        assertNull(exitInfoPluginStore.load())
    }

    /**
     * Null should be returned for invalid file contents
     */
    @Test
    fun readInvalidFileContents() {
        file.writeText("{\"hamster\": 2}")
        assertNull(exitInfoPluginStore.load())
    }

    /**
     * Information should be returned for valid files
     */
    @Test
    fun readValidFileContents() {
        file.writeText("12345")
        val info = requireNotNull(exitInfoPluginStore.load())
        assertEquals(12345, info)
    }

    @Test
    fun writableFile() {
        exitInfoPluginStore.persist(12345)
        val pid = file.readText()
        assertEquals("12345", pid)
    }

    @Test
    fun nonWritableFile() {
        file.apply {
            delete()
            createNewFile()
            setWritable(false)
        }
        assertNull(exitInfoPluginStore.load())
    }
}
