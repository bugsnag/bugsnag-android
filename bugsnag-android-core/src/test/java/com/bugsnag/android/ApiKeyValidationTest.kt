package com.bugsnag.android

import org.junit.Test

class ApiKeyValidationTest {

    @Test(expected = IllegalArgumentException::class)
    fun testEmptyApiKey() {
        Configuration("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testWrongSizeApiKey() {
        Configuration("abfe05f")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNonHexApiKey() {
        Configuration("yej0492j55z92z2p")
    }
}
