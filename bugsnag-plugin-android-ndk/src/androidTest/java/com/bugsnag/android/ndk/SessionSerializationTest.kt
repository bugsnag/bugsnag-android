package com.bugsnag.android.ndk

import org.junit.Assert.assertEquals
import org.junit.Test

internal class SessionSerializationTest {

    companion object {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("bugsnag-ndk-test")
        }
    }

    external fun run(): String

    @Test
    fun testPassesNativeSuite() {
        val expected = loadJson("session_serialization.json")
        val json = run()
        assertEquals(expected, json)
    }
}
