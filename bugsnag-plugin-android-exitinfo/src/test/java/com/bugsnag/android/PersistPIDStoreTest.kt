package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

internal class PersistPIDStoreTest {

    lateinit var file: File
    private lateinit var persistPIDStore: PersistPIDStore

    @Before
    fun setUp() {
        val config = TestData().generateConfig()!!
        file = File(config.persistenceDirectory.value, "bugsnag-exit-reasons")
        file.delete()
        persistPIDStore = PersistPIDStore(config)
    }

    /**
     * Null should be returned for non-existent files
     */
    @Test
    fun readNonExistentFile() {
        assertNull(persistPIDStore.load())
    }

    /**
     * Null should be returned for empty files
     */
    @Test
    fun readEmptyFile() {
        file.createNewFile()
        assertNull(persistPIDStore.load())
    }

    /**
     * Null should be returned for invalid file contents
     */
    @Test
    fun readInvalidFileContents() {
        file.writeText("{\"hamster\": 2}")
        assertNull(persistPIDStore.load())
    }

    /**
     * Information should be returned for valid files
     */
    @Test
    fun readValidFileContents() {
        file.writeText("12345")
        val info = requireNotNull(persistPIDStore.load())
        assertEquals(12345, info)
    }

    @Test
    fun writableFile() {
        persistPIDStore.persist(12345)
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
        assertNull(persistPIDStore.load())
    }
}
