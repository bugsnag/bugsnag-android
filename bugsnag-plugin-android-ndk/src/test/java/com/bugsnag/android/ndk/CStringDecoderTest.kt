package com.bugsnag.android.ndk

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

class CStringDecoderTest {
    @Test
    fun testAscii7Compatible() {
        val buffer = ByteBuffer.wrap(
            byteArrayOf(
                0x63, 0x6f, 0x6d, 0x2e, 0x65, 0x78, 0x61, 0x6d,
                0x70, 0x6c, 0x65, 0x2e, 0x62, 0x75, 0x67, 0x73,
                0x6e, 0x61, 0x67, 0x2e, 0x61, 0x6e, 0x64, 0x72,
                0x6f, 0x69, 0x64, 0x00, 0x00, 0x00, 0x00, 0x00
            )
        )

        assertEquals("com.example.bugsnag.android", buffer.getCString(buffer.remaining()))
    }

    @Test
    fun testEmptyString() {
        val buffer = ByteBuffer.allocate(64)
        assertEquals("", buffer.getCString(64))
        assertEquals(0, buffer.remaining())
    }

    @Test
    fun testNonAscii7Compatible() {
        val buffer = ByteBuffer.wrap(extendedBytes)
        assertEquals("はい、これは機械翻訳で書かれています", buffer.getCString(buffer.remaining()))
    }

    @Test
    fun testInvalidStrings() {
        val buffer = ByteBuffer.wrap(extendedBytes)
        assertEquals("はい、これは機械翻訳で書かれていま�", buffer.getCString(extendedBytes.indexOf(0) - 1))

        buffer.rewind()
        buffer.put(16, 32)
        assertEquals("はい、これ�㠯機械翻訳で書かれていま�", buffer.getCString(extendedBytes.indexOf(0) - 1))
    }

    @Test
    fun testGreekStrings() {
        val buffer = ByteBuffer.wrap(greekBytes)
        assertEquals("ναι, αυτό γράφτηκε με αυτόματη μετάφραση", buffer.getCString(buffer.remaining()))
    }
    @Test
    fun testInvalidGreekStrings() {
        val buffer = ByteBuffer.wrap(greekBytes)
        assertEquals("ναι, αυτό γράφτηκε με αυτόματη μετάφρασ�", buffer.getCString(greekBytes.indexOf(0) - 1))

        buffer.rewind()
        buffer.put(9, 32)
        assertEquals("ναι, �Πυτό γράφτηκε με αυτόματη μετάφρασ�", buffer.getCString(greekBytes.indexOf(0) - 1))
    }

    private val greekBytes = byteArrayOf(
        -50, -67, -50, -79, -50, -71, 44, 32,
        -50, -79, -49, -123, -49, -124, -49, -116,
        32, -50, -77, -49, -127, -50, -84, -49,
        -122, -49, -124, -50, -73, -50, -70, -50,
        -75, 32, -50, -68, -50, -75, 32, -50,
        -79, -49, -123, -49, -124, -49, -116, -50,
        -68, -50, -79, -49, -124, -50, -73, 32,
        -50, -68, -50, -75, -49, -124, -50, -84,
        -49, -122, -49, -127, -50, -79, -49, -125,
        -50, -73,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    )

    private val extendedBytes = byteArrayOf(
        -29, -127, -81, -29, -127, -124, -29, -128,
        -127, -29, -127, -109, -29, -126, -116, -29,
        -127, -81, -26, -87, -97, -26, -94, -80,
        -25, -65, -69, -24, -88, -77, -29, -127,
        -89, -26, -101, -72, -29, -127, -117, -29,
        -126, -116, -29, -127, -90, -29, -127, -124,
        -29, -127, -66, -29, -127, -103,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    )
}
