package com.bugsnag.android.ndk

import org.junit.Assert.assertEquals
import org.junit.Test

internal class SeverityReasonSerializationTest {

    companion object {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("bugsnag-ndk-test")
        }
    }

    external fun run(): String

    @Test
    fun testPassesNativeSuite() {
        val expected = loadJson("severity_reason_serialization.json")
        val json = run()
        assertEquals(expected, json)
    }
}
