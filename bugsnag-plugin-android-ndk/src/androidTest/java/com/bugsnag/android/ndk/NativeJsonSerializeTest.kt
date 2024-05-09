package com.bugsnag.android.ndk

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class NativeJsonSerializeTest {

    companion object {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("bugsnag-ndk-test")
        }
    }

    private val path = File(System.getProperty("java.io.tmpdir"), this::class.simpleName!!)

    @Before
    fun setupTmpdir() {
        path.mkdirs()
    }

    @After
    fun deleteTmpdir() {
        path.deleteRecursively()
    }

    external fun run(outputDir: String): Int

    @Test
    fun testPassesNativeSuite() {
        verifyNativeRun(run(path.absolutePath))
        val jsonFile = path.listFiles()!!.maxByOrNull { it.lastModified() }!!
        val expected = loadJson("event_serialization.json")
        assertEquals(expected, jsonFile.readText())
    }
}
