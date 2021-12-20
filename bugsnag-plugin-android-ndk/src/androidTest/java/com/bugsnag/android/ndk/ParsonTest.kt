package com.bugsnag.android.ndk

import org.junit.Assert.assertEquals
import org.junit.Test

internal class ParsonTest {

    @Test
    fun testMidrangeIntegerValueSerialization() {
        // values within the range  [-2^53 + 1, 2^53 - 1] (per RFC 7159) can be
        // represented directly
        assertEquals("[0]", longToJsonArray(0L))
        val numbers = arrayOf(
            29L,
            8409L,
            410144L,
            8388609L,
            16777216L,
            90772230L,
            2086473728L,
            8589934591L
        )
        numbers.forEach { value ->
            assertEquals("[$value]", longToJsonArray(value))
            assertEquals("[-$value]", longToJsonArray(-value))
        }

        val upperLimit = 2.toBigInteger().pow(53) - 1.toBigInteger()
        assertEquals("[$upperLimit]", longToJsonArray(upperLimit.toLong()))

        val lowerLimit = -2.toBigInteger().pow(53) + 1.toBigInteger()
        assertEquals("[$lowerLimit]", longToJsonArray(lowerLimit.toLong()))
    }

    @Test
    fun testBigAndSmallIntegerSerialization() {
        // very big and small values (>= 2^53 or <= -2^53, per RFC7159)
        // require alternate representation

        // This should be 9007199254740995. Using the default number type
        // (double) results in the value being interpreted as 9007199254740996
        // due to precision loss.
        val biggerInt = 2.toBigInteger().pow(53) + 3.toBigInteger()

        assertEquals("[\"$biggerInt\"]", longToJsonArray(biggerInt.toLong()))
        assertEquals("[\"-$biggerInt\"]", longToJsonArray(-biggerInt.toLong()))
    }

    // Converts a long to an array containing that value
    external fun longToJsonArray(value: Long): String

    companion object NativeLibs {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("bugsnag-ndk-test")
        }
    }
}
