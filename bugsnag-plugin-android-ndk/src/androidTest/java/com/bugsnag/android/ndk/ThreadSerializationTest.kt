package com.bugsnag.android.ndk

import org.junit.Assert
import org.junit.Test

class ThreadSerializationTest {

    companion object {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("bugsnag-ndk-test")
        }
    }

    external fun run(): String

    @Test
    fun testPassesNativeSuite() {
        val expected = loadJson("thread_serialization.json")
        val json = run()
        Assert.assertEquals(expected, json)
    }
}
