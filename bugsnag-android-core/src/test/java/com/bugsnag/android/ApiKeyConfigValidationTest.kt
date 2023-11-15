package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiKeyConfigValidationTest {

    @Test
    fun testWrongSizeApiKey() {
        val config = Configuration("abfe05f")
        assertEquals("abfe05f", config.apiKey)
    }

    @Test
    fun testNonHexApiKey() {
        val config = Configuration("yej0492j55z92z2p")
        assertEquals("yej0492j55z92z2p", config.apiKey)
    }

    @Test
    fun testSettingWrongSizeApiKey() {
        val config = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        config.apiKey = "abfe05f"
        assertEquals("abfe05f", config.apiKey)
    }

    @Test
    fun testSettingNonHexApiKey() {
        val config = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        config.apiKey = "yej0492j55z92z2p"
        assertEquals("yej0492j55z92z2p", config.apiKey)
    }

    @Test
    fun setApiKey() {
        val config = Configuration("5d1ec5bd39a74caa1267142706a7fb21")
        assertEquals("5d1ec5bd39a74caa1267142706a7fb21", config.apiKey)
        config.apiKey = "000005bd39a74caa1267142706a7fb21"
        assertEquals("000005bd39a74caa1267142706a7fb21", config.apiKey)
    }
}
