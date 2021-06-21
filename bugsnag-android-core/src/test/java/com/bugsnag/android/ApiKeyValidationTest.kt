package com.bugsnag.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiKeyValidationTest {

    @Test(expected = IllegalArgumentException::class)
    fun testNullApiKey() {
        Configuration.isInvalidApiKey(null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testEmptyApiKey() {
        Configuration.isInvalidApiKey("")
    }

    @Test
    fun testWrongSizeApiKey() {
        assertTrue(Configuration.isInvalidApiKey("abfe05f"))
        assertTrue(Configuration.isInvalidApiKey("5d1ec5bd39a74caa1267142706a7fb212"))
    }

    @Test
    fun testSettingNonHexApiKey() {
        assertTrue(Configuration.isInvalidApiKey("5d1ec5bd3Ga74caa1267142706a7fb21"))
        assertTrue(Configuration.isInvalidApiKey("5d1ec5bd39a7%caa1267_42706P7fb212"))
        assertFalse(Configuration.isInvalidApiKey("5d1ec5bd39a74caa1267142706a7fb21"))
    }

    @Test
    fun setApiKey() {
        assertFalse(Configuration.isInvalidApiKey("5d1ec5bd39a74caa1267142706a7fb21"))
        assertFalse(Configuration.isInvalidApiKey("000005bd39a74caa1267142706a7fb21"))
    }
}
