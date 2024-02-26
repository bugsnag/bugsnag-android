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
        val stackFrame1 = Stackframe(null, null, null, null)
        val stackFrame2 = Stackframe(null, null, null, null)
        val stackFrame3 = Stackframe(null, null, null, null)
        val stackFrame4 = Stackframe(null, null, null, null)
        val stackFrame5 = Stackframe(null, null, null, null)
        val stackFrame6 = Stackframe(null, null, null, null)
        val stackFrame7 = Stackframe(null, null, null, null)

        val error = event.errors.single()
        assertEquals("SIGSEGV", error.errorClass)
        assertEquals("Segmentation violation (invalid memory reference)", error.errorMessage)
        assertEquals(ErrorType.UNKNOWN, error.type)

        stackFrame1.frameAddress = 512876502024L
        stackFrame1.symbolAddress = 512876502020L
        stackFrame1.loadAddress = 512876498944L
        stackFrame1.lineNumber = 3080L
        stackFrame1.file = "/data/app/~~dpOrZdWcDXB7AvWSQq_ToA==/com.example.bugsnag.android-qrHfhc0chd9kQAkU0AxRKQ==/lib/arm64/libentrypoint.so"
        stackFrame1.method = "Java_com_example_bugsnag_android_BaseCrashyActivity_crashFromCXX"
        stackFrame1.codeIdentifier = "bfc2827bededb48c287bc9a4e1c61d41514d6a8b"

        stackFrame2.frameAddress = 514965598256L
        stackFrame2.symbolAddress = 514965598112L
        stackFrame2.loadAddress = 514964062208L
        stackFrame2.lineNumber = 3633200L
        stackFrame2.file = "/apex/com.android.art/lib64/libart.so"
        stackFrame2.method = "art_quick_generic_jni_trampoline"
        stackFrame2.codeIdentifier = "b10f5696fea1b32039b162aef3850ed3"

        stackFrame3.frameAddress = 514967776432L
        stackFrame3.symbolAddress = 514967772416L
        stackFrame3.loadAddress = 514964062208L
        stackFrame3.lineNumber = 5811376L
        stackFrame3.file = "/apex/com.android.art/lib64/libart.so"
        stackFrame3.method = "nterp_helper"
        stackFrame3.codeIdentifier = "b10f5696fea1b32039b162aef3850ed3"

        stackFrame4.frameAddress = 512895163634L
        stackFrame4.symbolAddress = 512895163634L
        stackFrame4.loadAddress = 512894267392L
        stackFrame4.lineNumber = 896242L
        stackFrame4.file = "/data/app/~~dpOrZdWcDXB7AvWSQq_ToA==/com.example.bugsnag.android-qrHfhc0chd9kQAkU0AxRKQ==/oat/arm64/base.odex"
        stackFrame4.method = "0x776aef1cf2"
        stackFrame4.codeIdentifier = ""

        stackFrame5.frameAddress = 514967772468L
        stackFrame5.symbolAddress = 514967772416L
        stackFrame5.loadAddress = 514964062208L
        stackFrame5.lineNumber = 5807412L
        stackFrame5.file = "/apex/com.android.art/lib64/libart.so"
        stackFrame5.method = "nterp_helper"
        stackFrame5.codeIdentifier = "b10f5696fea1b32039b162aef3850ed3"

        stackFrame6.frameAddress = 512895084840L
        stackFrame6.symbolAddress = 512895084840L
        stackFrame6.loadAddress = 512894267392L
        stackFrame6.lineNumber = 817448L
        stackFrame6.file = "/data/app/~~dpOrZdWcDXB7AvWSQq_ToA==/com.example.bugsnag.android-qrHfhc0chd9kQAkU0AxRKQ==/oat/arm64/base.odex"
        stackFrame6.method = "0x776aede928"
        stackFrame6.codeIdentifier = ""

        stackFrame7.frameAddress = 1919189480L
        stackFrame7.symbolAddress = 1919189024L
        stackFrame7.loadAddress = 1913180160L
        stackFrame7.lineNumber = 8016360L
        stackFrame7.file = "/system/framework/arm64/boot-framework.oat"
        stackFrame7.method = "android.view.View.performClick"
        stackFrame7.codeIdentifier = "d78a45b435dbd99c4526a33e3674ae4ee6ada328"

        assertEquals(stackFrame1.frameAddress, error.stacktrace.first().frameAddress)
        assertEquals(stackFrame1.symbolAddress, error.stacktrace.first().symbolAddress)
        assertEquals(stackFrame1.loadAddress, error.stacktrace.first().loadAddress)
        assertEquals(stackFrame1.lineNumber, error.stacktrace.first().lineNumber)
        assertEquals(stackFrame1.file, error.stacktrace.first().file)
        assertEquals(stackFrame1.method, error.stacktrace.first().method)
        assertEquals(stackFrame1.codeIdentifier, error.stacktrace.first().codeIdentifier)

        assertEquals(stackFrame2.frameAddress, error.stacktrace[1].frameAddress)
        assertEquals(stackFrame2.symbolAddress, error.stacktrace[1].symbolAddress)
        assertEquals(stackFrame2.loadAddress, error.stacktrace[1].loadAddress)
        assertEquals(stackFrame2.lineNumber, error.stacktrace[1].lineNumber)
        assertEquals(stackFrame2.file, error.stacktrace[1].file)
        assertEquals(stackFrame2.method, error.stacktrace[1].method)
        assertEquals(stackFrame2.codeIdentifier, error.stacktrace[1].codeIdentifier)

        assertEquals(stackFrame3.frameAddress, error.stacktrace[2].frameAddress)
        assertEquals(stackFrame3.symbolAddress, error.stacktrace[2].symbolAddress)
        assertEquals(stackFrame3.loadAddress, error.stacktrace[2].loadAddress)
        assertEquals(stackFrame3.lineNumber, error.stacktrace[2].lineNumber)
        assertEquals(stackFrame3.file, error.stacktrace[2].file)
        assertEquals(stackFrame3.method, error.stacktrace[2].method)
        assertEquals(stackFrame3.codeIdentifier, error.stacktrace[2].codeIdentifier)

        assertEquals(stackFrame4.frameAddress, error.stacktrace[3].frameAddress)
        assertEquals(stackFrame4.symbolAddress, error.stacktrace[3].symbolAddress)
        assertEquals(stackFrame4.loadAddress, error.stacktrace[3].loadAddress)
        assertEquals(stackFrame4.lineNumber, error.stacktrace[3].lineNumber)
        assertEquals(stackFrame4.file, error.stacktrace[3].file)
        assertEquals(stackFrame4.method, error.stacktrace[3].method)
        assertEquals(stackFrame4.codeIdentifier, error.stacktrace[3].codeIdentifier)

        assertEquals(stackFrame5.frameAddress, error.stacktrace[4].frameAddress)
        assertEquals(stackFrame5.symbolAddress, error.stacktrace[4].symbolAddress)
        assertEquals(stackFrame5.loadAddress, error.stacktrace[4].loadAddress)
        assertEquals(stackFrame5.lineNumber, error.stacktrace[4].lineNumber)
        assertEquals(stackFrame5.file, error.stacktrace[4].file)
        assertEquals(stackFrame5.method, error.stacktrace[4].method)
        assertEquals(stackFrame5.codeIdentifier, error.stacktrace[4].codeIdentifier)

        assertEquals(stackFrame6.frameAddress, error.stacktrace[5].frameAddress)
        assertEquals(stackFrame6.symbolAddress, error.stacktrace[5].symbolAddress)
        assertEquals(stackFrame6.loadAddress, error.stacktrace[5].loadAddress)
        assertEquals(stackFrame6.lineNumber, error.stacktrace[5].lineNumber)
        assertEquals(stackFrame6.file, error.stacktrace[5].file)
        assertEquals(stackFrame6.method, error.stacktrace[5].method)
        assertEquals(stackFrame6.codeIdentifier, error.stacktrace[5].codeIdentifier)

        assertEquals(stackFrame7.frameAddress, error.stacktrace[6].frameAddress)
        assertEquals(stackFrame7.symbolAddress, error.stacktrace[6].symbolAddress)
        assertEquals(stackFrame7.loadAddress, error.stacktrace[6].loadAddress)
        assertEquals(stackFrame7.lineNumber, error.stacktrace[6].lineNumber)
        assertEquals(stackFrame7.file, error.stacktrace[6].file)
        assertEquals(stackFrame7.method, error.stacktrace[6].method)
        assertEquals(stackFrame7.codeIdentifier, error.stacktrace[6].codeIdentifier)
    }
}
