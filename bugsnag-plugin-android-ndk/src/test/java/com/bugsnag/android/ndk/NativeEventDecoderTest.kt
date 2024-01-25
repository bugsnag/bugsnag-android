package com.bugsnag.android.ndk

import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.ByteBuffer

class NativeEventDecoderTest {
    @Test
    fun testAarch64Emulator() {
        val event = NativeEventDecoder.decodeEventFromBytes(readEventBytes("/aarch64-emulator.crash_dump"))
        assertNotNull(event)
    }

    private fun readEventBytes(name: String): ByteBuffer {
        return ByteBuffer.wrap(this::class.java.getResourceAsStream(name)!!.readBytes())
    }
}
