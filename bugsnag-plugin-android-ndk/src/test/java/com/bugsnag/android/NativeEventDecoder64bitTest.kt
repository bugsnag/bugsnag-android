package com.bugsnag.android

import com.bugsnag.android.ndk.NativeArch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class NativeEventDecoder64bitTest {
    private val crashDump64BitData =
        this::class.java.getResourceAsStream("/aarch64-emulator.crash_dump")!!.readBytes()

    private val event = mock(Event::class.java)
    private val session = mock(Session::class.java)
    private val notifier = mock(Notifier::class.java)
    private val device = mock(DeviceWithState::class.java)

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

        // device info
        assertEquals(34, runtimeVersions["apiLevel"])
        assertEquals("UE1A.230829.030", runtimeVersions["osBuild"])
        verify(device).orientation = "portrait"
        verify(device).time = Date(1706172734 * 1000L)
        verify(device).id = "dba44d63-31d6-4770-9617-8d8782213d23"
        verify(device).jailbroken = false
        verify(device).locale = "en_US"
        verify(device).manufacturer = "Google"
        verify(device).model = "sdk_gphone64_arm64"
        verify(device).osVersion = "14"
        verify(device).osName = "android"
        verify(device).totalMemory = 0L

        // user info
        verify(event, Mockito.times(1)).setUser("999999", "ndk override", "j@ex.co")
    }
}
