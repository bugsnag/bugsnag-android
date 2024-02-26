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
    private val logger = mock(Logger::class.java)

    private lateinit var data: ByteBuffer
    private val runtimeVersions = mutableMapOf<String, Any>()

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
        data = ByteBuffer.wrap(crashDumpData)
        data.order(ByteOrder.LITTLE_ENDIAN)
        `when`(event.session).thenReturn(session)
        `when`(session.notifier).thenReturn(notifier)
        `when`(event.device).thenReturn(device)
        `when`(device.runtimeVersions).thenReturn(runtimeVersions)
        val errors = Error.createError(RuntimeException(), emptySet(), logger)
        `when`(event.errors).thenReturn(errors)
        NativeEventDecoder.decodeEventFromBytes(data, event)

        verifyNotifierDecode()
        assertAppInfo()
        verifyDeviceInfoDecode()
        verifyUserInfoDecode()
        verifyErrorDecode()
    }

    private fun verifyNotifierDecode() {
        verify(notifier).name = ""
        verify(notifier).version = ""
        verify(notifier).url = ""
    }

    private fun assertAppInfo() {
        val captor = ArgumentCaptor.forClass(AppWithState::class.java)
        verify(event).app = captor.capture()
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
    }

    private fun verifyDeviceInfoDecode() {
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
    }

    private fun verifyUserInfoDecode() {
        verify(event, times(1)).setUser("999999", "ndk override", "j@ex.co")
    }

    private fun verifyErrorDecode() {
        val error = event.errors.single()
        assertEquals("SIGSEGV", error.errorClass)
        assertEquals("Segmentation violation (invalid memory reference)", error.errorMessage)
        assertEquals(ErrorType.UNKNOWN, error.type)

        val stackFrame1 = error.stacktrace[0]
        val stackFrame2 = error.stacktrace[1]

        assertEquals(1285807130L, stackFrame1.frameAddress)
        assertEquals(1285807124L, stackFrame1.symbolAddress)
        assertEquals(1285804032L, stackFrame1.loadAddress)
        assertEquals(3098L, stackFrame1.lineNumber)
        assertEquals(
            "/data/data/com.example.bugsnag.android/lib/libentrypoint.so",
            stackFrame1.file
        )
        assertEquals(
            "Java_com_example_bugsnag_android_BaseCrashyActivity_crashFromCXX",
            stackFrame1.method
        )
        assertEquals("5ddb429dfa12daf935fbe29b6d2d498a5740e0eb", stackFrame1.codeIdentifier)

        assertEquals(1082654128L, stackFrame2.frameAddress)
        assertEquals(1082654016L, stackFrame2.symbolAddress)
        assertEquals(1082527744L, stackFrame2.loadAddress)
        assertEquals(126384L, stackFrame2.lineNumber)
        assertEquals("/system/lib/libdvm.so", stackFrame2.file)
        assertEquals("dvmPlatformInvoke", stackFrame2.method)
        assertEquals("", stackFrame2.codeIdentifier)
    }
}
