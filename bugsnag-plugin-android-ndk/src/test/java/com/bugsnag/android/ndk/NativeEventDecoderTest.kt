package com.bugsnag.android.ndk

import com.bugsnag.android.Event
import com.bugsnag.android.NativeEventDecoder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import java.nio.ByteBuffer

@RunWith(MockitoJUnitRunner::class)
class NativeEventDecoderTest {
    private val sample32BitData = this::class.java.getResourceAsStream("/arm32-droid-razr.crash_dump")!!.readBytes()

     private val event = mock(Event::class.java)


    @Before
    fun setupArchitecture() {
        NativeArch._is32Bit = true
    }

    @After
    fun unSetupArchitecture() {
        NativeArch._is32Bit = null
    }

    @Test
    fun test32BitPrimitives() {
        val data = ByteBuffer.wrap(sample32BitData)
        NativeEventDecoder.decodeEventFromBytes(data, event)
        assertEquals("", event.app.id)
    }
}
