package com.bugsnag.android

import com.bugsnag.android.internal.isInvalidApiKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ApiKeyValidationTest {

    @Test(expected = IllegalArgumentException::class)
    fun testEmptyApiKey() {
        isInvalidApiKey("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNullApiKey() {
        isInvalidApiKey(null)
    }

    @Test
    fun testWrongSizeApiKey() {
        assertTrue(isInvalidApiKey("abfe05f"))
        assertTrue(isInvalidApiKey("5d1ec5bd39a74caa1267142706a7fb212"))
    }

    @Test
    fun testSettingNonHexApiKey() {
        assertTrue(isInvalidApiKey("5d1ec5bd3Ga74caa1267142706a7fb21"))
        assertTrue(isInvalidApiKey("5d1ec5bd39a7%caa1267_42706P7fb212"))
        assertFalse(isInvalidApiKey("5d1ec5bd39a74caa1267142706a7fb21"))
    }

    @Test
    fun setApiKey() {
        assertFalse(isInvalidApiKey("5d1ec5bd39a74caa1267142706a7fb21"))
        assertFalse(isInvalidApiKey("000005bd39a74caa1267142706a7fb21"))
    }
}
