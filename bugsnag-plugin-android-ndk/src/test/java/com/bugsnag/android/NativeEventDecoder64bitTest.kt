package com.bugsnag.android

import com.bugsnag.android.ndk.NativeArch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(MockitoJUnitRunner::class)
class NativeEventDecoder64bitTest {
    private val crashDump64BitData =
        this::class.java.getResourceAsStream("/aarch64-emulator.crash_dump")!!.readBytes()

    private val event = mock(Event::class.java)
    private val session = mock(Session::class.java)
    private val notifier = mock(Notifier::class.java)

    @Before
    fun setupArchitecture() {
        NativeArch._is32Bit = false
    }

    @After
    fun unSetupArchitecture() {
        NativeArch._is32Bit = null
    }

    @Test
    fun testNativeEventDecode() {
        val data = ByteBuffer.wrap(crashDump64BitData)
        data.order(ByteOrder.LITTLE_ENDIAN)
        val captor = ArgumentCaptor.forClass(AppWithState::class.java)
        `when`(event.session).thenReturn(session)
        `when`(session.notifier).thenReturn(notifier)

        NativeEventDecoder.decodeEventFromBytes(data, event)

        verify(event).app = captor.capture()

        verify(notifier).name = ""
        verify(notifier).version = ""
        verify(notifier).url = ""

        assertEquals("com.example.bugsnag.android", captor.value.id)
        assertEquals("production", captor.value.releaseStage)
        assertEquals("android", captor.value.type)
        assertEquals("1.0", captor.value.version)
        assertEquals(1L, captor.value.versionCode)
        assertEquals("c4cedba4-9c34-48c7-a26e-d658c1e98c67", captor.value.buildUuid)
        assertEquals(1070L, captor.value.duration)
        assertEquals(1000L, captor.value.durationInForeground)
        assertEquals(true, captor.value.inForeground)
        assertEquals(true, captor.value.isLaunching)
        assertEquals("arm64", captor.value.binaryArch)
    }
}
