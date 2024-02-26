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
    private val logger = mock(Logger::class.java)

    private lateinit var data: ByteBuffer
    private val runtimeVersions = mutableMapOf<String, Any>()

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
        data = ByteBuffer.wrap(crashDump64BitData)
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

    private fun verifyDeviceInfoDecode() {
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
    }

    private fun verifyUserInfoDecode() {
        verify(event, Mockito.times(1)).setUser("999999", "ndk override", "j@ex.co")
    }

    private fun verifyErrorDecode() {
        val error = event.errors.single()
        assertEquals("SIGSEGV", error.errorClass)
        assertEquals("Segmentation violation (invalid memory reference)", error.errorMessage)
        assertEquals(ErrorType.UNKNOWN, error.type)

        val stackFrame1 = error.stacktrace[0]
        val stackFrame2 = error.stacktrace[1]
        val stackFrame3 = error.stacktrace[2]
        val stackFrame4 = error.stacktrace[3]
        val stackFrame5 = error.stacktrace[4]
        val stackFrame6 = error.stacktrace[5]
        val stackFrame7 = error.stacktrace[6]

        assertEquals(512876502024L, stackFrame1.frameAddress)
        assertEquals(512876502020L, stackFrame1.symbolAddress)
        assertEquals(512876498944L, stackFrame1.loadAddress)
        assertEquals(3080L, stackFrame1.lineNumber)
        assertEquals(
            "/data/app/~~dpOrZdWcDXB7AvWSQq_ToA==/com.example.bugsnag.android-qrHfhc0chd9kQAkU0AxRKQ==/lib/arm64/libentrypoint.so",
            stackFrame1.file
        )
        assertEquals(
            "Java_com_example_bugsnag_android_BaseCrashyActivity_crashFromCXX",
            stackFrame1.method
        )
        assertEquals("bfc2827bededb48c287bc9a4e1c61d41514d6a8b", stackFrame1.codeIdentifier)

        assertEquals(514965598256L, stackFrame2.frameAddress)
        assertEquals(514965598112L, stackFrame2.symbolAddress)
        assertEquals(514964062208L, stackFrame2.loadAddress)
        assertEquals(3633200L, stackFrame2.lineNumber)
        assertEquals("/apex/com.android.art/lib64/libart.so", stackFrame2.file)
        assertEquals("art_quick_generic_jni_trampoline", stackFrame2.method)
        assertEquals("b10f5696fea1b32039b162aef3850ed3", stackFrame2.codeIdentifier)

        assertEquals(514967776432L, stackFrame3.frameAddress)
        assertEquals(514967772416L, stackFrame3.symbolAddress)
        assertEquals(514964062208L, stackFrame3.loadAddress)
        assertEquals(5811376L, stackFrame3.lineNumber)
        assertEquals("/apex/com.android.art/lib64/libart.so", stackFrame3.file)
        assertEquals("nterp_helper", stackFrame3.method)
        assertEquals("b10f5696fea1b32039b162aef3850ed3", stackFrame3.codeIdentifier)

        assertEquals(512895163634L, stackFrame4.frameAddress)
        assertEquals(512895163634L, stackFrame4.symbolAddress)
        assertEquals(512894267392L, stackFrame4.loadAddress)
        assertEquals(896242L, stackFrame4.lineNumber)
        assertEquals(
            "/data/app/~~dpOrZdWcDXB7AvWSQq_ToA==/com.example.bugsnag.android-qrHfhc0chd9kQAkU0AxRKQ==/oat/arm64/base.odex",
            stackFrame4.file
        )
        assertEquals("0x776aef1cf2", stackFrame4.method)
        assertEquals("", stackFrame4.codeIdentifier)

        assertEquals(514967772468L, stackFrame5.frameAddress)
        assertEquals(514967772416L, stackFrame5.symbolAddress)
        assertEquals(514964062208L, stackFrame5.loadAddress)
        assertEquals(5807412L, stackFrame5.lineNumber)
        assertEquals("/apex/com.android.art/lib64/libart.so", stackFrame5.file)
        assertEquals("nterp_helper", stackFrame5.method)
        assertEquals("b10f5696fea1b32039b162aef3850ed3", stackFrame5.codeIdentifier)

        assertEquals(512895084840L, stackFrame6.frameAddress)
        assertEquals(512895084840L, stackFrame6.symbolAddress)
        assertEquals(512894267392L, stackFrame6.loadAddress)
        assertEquals(817448L, stackFrame6.lineNumber)
        assertEquals(
            "/data/app/~~dpOrZdWcDXB7AvWSQq_ToA==/com.example.bugsnag.android-qrHfhc0chd9kQAkU0AxRKQ==/oat/arm64/base.odex",
            stackFrame6.file
        )
        assertEquals("0x776aede928", stackFrame6.method)
        assertEquals("", stackFrame6.codeIdentifier)

        assertEquals(1919189480L, stackFrame7.frameAddress)
        assertEquals(1919189024L, stackFrame7.symbolAddress)
        assertEquals(1913180160L, stackFrame7.loadAddress)
        assertEquals(8016360L, stackFrame7.lineNumber)
        assertEquals("/system/framework/arm64/boot-framework.oat", stackFrame7.file)
        assertEquals("android.view.View.performClick", stackFrame7.method)
        assertEquals("d78a45b435dbd99c4526a33e3674ae4ee6ada328", stackFrame7.codeIdentifier)
    }
}
