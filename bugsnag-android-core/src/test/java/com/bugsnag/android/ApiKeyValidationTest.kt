package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiKeyValidationTest {

    @Test
    fun testEmptyApiKey() {
        Configuration("")
    }

    @Test
    fun testWrongSizeApiKey() {
        Configuration("abfe05f")
    }

    @Test
    fun testNonHexApiKey() {
        Configuration("yej0492j55z92z2p")
    }

    @Test
    fun testSettingEmptyApiKey() {
        val config = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        config.apiKey = ""
    }

    @Test
    fun testSettingWrongSizeApiKey() {
        val config = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        config.apiKey = "abfe05f"
    }

    @Test
    fun testSettingNonHexApiKey() {
        val config = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        config.apiKey = "yej0492j55z92z2p"
    }

    @Test
    fun setApiKey() {
        val config = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        assertEquals("5d1ec5bd39a74caa1267142706a7fb21", config.apiKey)
        config.apiKey = "000005bd39a74caa1267142706a7fb21"
        assertEquals("000005bd39a74caa1267142706a7fb21", config.apiKey)
    }
}
