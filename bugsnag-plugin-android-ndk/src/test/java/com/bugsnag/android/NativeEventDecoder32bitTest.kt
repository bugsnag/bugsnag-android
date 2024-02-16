package com.bugsnag.android

import com.bugsnag.android.ndk.NativeArch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class NativeEventDecoder32bitTest {
    private val crashDumpData =
        this::class.java.getResourceAsStream("/arm32-droid-razr.crash_dump")!!.readBytes()

    private val event = mock(Event::class.java)
    private val session = mock(Session::class.java)
    private val notifier = mock(Notifier::class.java)
    private val device = mock(DeviceWithState::class.java)

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
        val runtimeVersions = mutableMapOf<String, Any>()
        `when`(event.session).thenReturn(session)
        `when`(session.notifier).thenReturn(notifier)
        `when`(event.device).thenReturn(device)
        `when`(device.runtimeVersions).thenReturn(runtimeVersions)

        NativeEventDecoder.decodeEventFromBytes(data, event)
        verify(event).app = captor.capture()

        // notifier
        verify(notifier).name = ""
        verify(notifier).version = ""
        verify(notifier).url = ""

        // app info
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

        // device info
        assertEquals(15, runtimeVersions["apiLevel"])
        assertEquals("6.7.3-94_SPI-324", runtimeVersions["osBuild"])
        verify(device).orientation = "portrait"
        verify(device).time = Date(1339585022 * 1000L)
        verify(device).id = "deddc379-5a77-4f2c-b21c-b24baca697f6"
        verify(device).jailbroken = false
        verify(device).locale = "en_GB"
        verify(device).manufacturer = "motorola"
        verify(device).model = "XT910"
        verify(device).osVersion = "4.0.4"
        verify(device).osName = "android"
        verify(device).totalMemory = 0L

        // user info
        verify(event, times(1)).setUser("999999", "ndk override", "j@ex.co")
    }
}
