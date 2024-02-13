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
class NativeEventDecoder32bitTest {
    private val crashDumpData =
        this::class.java.getResourceAsStream("/arm32-droid-razr.crash_dump")!!.readBytes()

    private val event = mock(Event::class.java)
    private val session = mock(Session::class.java)
    private val notifier = mock(Notifier::class.java)

    @Before
    fun setupArchitecture() {
        NativeArch._is32Bit = true
    }

    @After
    fun unSetupArchitecture() {
        NativeArch._is32Bit = null
    }

    @Test
    fun testNativeEventDecode() {
        val data = ByteBuffer.wrap(crashDumpData)
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
        assertEquals("development", captor.value.releaseStage)
        assertEquals("android", captor.value.type)
        assertEquals("1.0", captor.value.version)
        assertEquals(1L, captor.value.versionCode)
        assertEquals("4450ef3c-ac1f-47e1-9fc3-3a2d90e59dc4", captor.value.buildUuid)
        assertEquals(4340L, captor.value.duration)
        assertEquals(4000L, captor.value.durationInForeground)
        assertEquals(true, captor.value.inForeground)
        assertEquals(true, captor.value.isLaunching)
        assertEquals("arm32", captor.value.binaryArch)

//        assertEquals("arm32", deviceCaptor.value.cpuAbi)

    }
}
