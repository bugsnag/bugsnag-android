package com.bugsnag.android.ndk

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CPrimitivesTest32Bit {
    private val sample32BitData = byteArrayOf(
        0x0d, 0, 0, 0, 0, 0, 0, 0, // taken from an event_header (int, int)
        0xf4.toByte(), 0x10, 0, 0, 0, 0, 0, 0, // bsg_app_info.duration int64_t
        0xa0.toByte(), 0x0f, 0, 0, 0, 0, 0, 0, // bsg_app_info.duration_in_foreground int64_t
        0x54, 0x01, 0, 0, 0, 0, 0, 0, // bsg_app_info.duration_ms_offset int64_t
        0x54, 0x01, 0, 0, // bsg_app_info.duration_in_foreground_ms_offset time_t
        0x01, // bsg_app_info.in_foreground
        0x00, // bsg_app_info.is_launching
    )

    @Before
    fun setupArchitecture() {
        NativeArch._is32Bit = true
    }

    @After
    fun unsetupArchitecture() {
        NativeArch._is32Bit = null
    }

    @Test
    fun test32BitPrimitives() {
        val data = ByteBuffer.wrap(sample32BitData)
        data.order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(13, data.getNativeInt())
        assertEquals(0, data.getNativeInt())

        assertEquals(4340, data.getLong())
        assertEquals(4000, data.getLong())
        assertEquals(340, data.getLong())
        assertEquals(340, data.getNativeTime())
        assertEquals(true, data.getNativeBool())
        assertEquals(false, data.getNativeBool())
    }
}
