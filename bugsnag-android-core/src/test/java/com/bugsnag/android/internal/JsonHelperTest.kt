package com.bugsnag.android.internal

import org.junit.Assert.assertEquals
import org.junit.Test

internal class JsonHelperTest {
    fun assertBidirectional(longValue: Long?, stringValue: String?) {
        assertEquals(stringValue, JsonHelper.ulongToHex(longValue))
        assertEquals(longValue, JsonHelper.jsonToLong(stringValue))
    }

    fun assertDecode(stringValue: String?, longValue: Long?) {
        assertEquals(longValue, JsonHelper.jsonToLong(stringValue))
    }

    @Test
    fun jsonLongConversions() {
        assertBidirectional(null, null)
        assertBidirectional(0, "0x0")
        assertBidirectional(1, "0x1")
        assertBidirectional(0x7fffffffffffffff, "0x7fffffffffffffff")
        assertBidirectional(-1, "0xffffffffffffffff")
        assertBidirectional(-0x7fffffffffffffff, "0x8000000000000001")
        assertBidirectional(-0x7fffffffffffffff - 1, "0x8000000000000000")

        assertDecode("", null)
        assertDecode("0", 0)
        assertDecode("1", 1)
        assertDecode("-1", -1)
        assertDecode("9223372036854775807", 9223372036854775807)
        assertDecode("-9223372036854775807", -9223372036854775807)
        assertDecode("-9223372036854775808", -9223372036854775807 - 1)
        assertDecode("9223372036854775808", -9223372036854775807 - 1)
        assertDecode("0x8000000000000000", -9223372036854775807 - 1)
        assertDecode("18446744073709551615", -1)
        assertDecode("0xffffffffffffffff", -1)

        var didThrow = false
        try {
            JsonHelper.jsonToLong(false)
        } catch (e: IllegalArgumentException) {
            didThrow = true
        } finally {
            assert(didThrow, { "Expected to throw IllegalArgumentException" })
        }
    }
}
