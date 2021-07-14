package com.bugsnag.android.ndk

import org.junit.Assert.assertEquals
import org.junit.Test

internal class BreadcrumbStateSerializationTest {

    companion object {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("bugsnag-ndk-test")
        }
    }

    external fun run(): String

    @Test
    fun testBreadcrumbSerialization() {
        val expected = loadJson("breadcrumbs_serialization.json")
        val json = run()
        assertEquals(expected, json)
    }
}
