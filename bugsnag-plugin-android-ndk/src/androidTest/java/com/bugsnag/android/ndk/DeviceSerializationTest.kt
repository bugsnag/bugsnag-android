package com.bugsnag.android.ndk

import org.junit.Assert.assertEquals
import org.junit.Test

internal class DeviceSerializationTest {

    companion object {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("bugsnag-ndk-test")
        }
    }

    external fun run(): String

    @Test
    fun testPassesNativeSuite() {
        val expected = loadJson("device_serialization.json")
        val json = run()
        assertEquals(expected, json)
    }
}
