package com.bugsnag.android.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Collections

class StringUtilsTest {
    private val shortString = "this is a short string"
    private val longString =
        "this is a really long string with lots of words, words are made out of characters... " +
            "we have a lot of character"
    private val trimmedLongString = "this is a really***<95> CHARS TRUNCATED***"

    /**
     * Check that StringUtils doesn't trim strings where the trim message would overflow the
     * maximum string length (ie: trimming would make the string > max)
     */
    @Test
    fun testTrimOverflow() {
        assertEquals(shortString, StringUtils.stringTrimmedTo(9, shortString))
        assertEquals(longString, StringUtils.stringTrimmedTo(longString.length - 8, longString))
    }

    @Test
    fun testTrim() {
        assertEquals(
            trimmedLongString,
            StringUtils.stringTrimmedTo(16, longString)
        )
    }

    @Test
    fun testTrimMap() {
        val map = mutableMapOf(
            "short" to shortString,
            "long" to longString,
            "null" to null,
            "number" to 54321,
            "boolean" to false,
            "nested" to mutableMapOf(
                "short" to shortString,
                "long" to longString
            ),
            "list" to mutableListOf(
                shortString,
                longString
            ),
            "singleton" to Collections.singleton(longString),
            "immutableList" to Collections.unmodifiableList(
                listOf(
                    shortString,
                    longString,
                    12345
                )
            ),
            "immutableMap" to Collections.unmodifiableMap(
                mapOf(
                    "short" to shortString,
                    "long" to longString,
                )
            )
        )

        StringUtils.trimStringValuesTo(
            16, map
        )

        assertEquals(shortString, map["short"])
        assertEquals(trimmedLongString, map["long"])
        assertNull(map["null"])
        assertEquals(54321, map["number"])
        assertEquals(false, map["boolean"])

        val nested = map["nested"] as Map<*, *>
        assertEquals(shortString, nested["short"])
        assertEquals(trimmedLongString, nested["long"])
        assertNull(nested["null"])

        val list = map["list"] as List<*>
        assertEquals(shortString, list[0])
        assertEquals(trimmedLongString, list[1])

        val singleton = map["singleton"] as List<*>
        assertEquals(trimmedLongString, singleton[0])

        val immutableList = map["immutableList"] as List<*>
        assertEquals(shortString, immutableList[0])
        assertEquals(trimmedLongString, immutableList[1])
        assertEquals(12345, immutableList[2])

        val immutableMap = map["immutableMap"] as Map<*, *>
        assertEquals(shortString, immutableMap["short"])
        assertEquals(trimmedLongString, immutableMap["long"])
    }
}
