package com.bugsnag.android

import android.content.Context
import com.bugsnag.android.BugsnagTestUtils.generateAppWithState
import com.bugsnag.android.BugsnagTestUtils.generateDeviceWithState
import com.bugsnag.android.internal.ImmutableConfig
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import java.nio.file.Files

/**
 * Verifies that method calls are forwarded onto the appropriate method on Client,
 * and that adequate sanitisation takes place.
 */
@RunWith(MockitoJUnitRunner::class)
internal class NativeInterfaceApiTest {

    @Mock
    lateinit var client: Client

    @Mock
    lateinit var immutableConfig: ImmutableConfig

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var appDataCollector: AppDataCollector

    @Mock
    lateinit var deviceDataCollector: DeviceDataCollector

    @Mock
    lateinit var sessionTracker: SessionTracker

    @Mock
    lateinit var eventStore: EventStore

    @Captor
    lateinit var eventCapture: ArgumentCaptor<Event>

    @Before
    fun setUp() {
        NativeInterface.setClient(client)
        `when`(client.config).thenReturn(immutableConfig)
        `when`(client.getSessionTracker()).thenReturn(sessionTracker)
        `when`(client.getEventStore()).thenReturn(eventStore)
        `when`(immutableConfig.apiKey).thenReturn("test-api-key")
        `when`(immutableConfig.endpoints).thenReturn(
            EndpointConfiguration(
                "http://notify.bugsnag.com",
                "http://sessions.bugsnag.com"
            )
        )
        `when`(immutableConfig.redactedKeys).thenReturn(emptySet())
        `when`(immutableConfig.sendThreads).thenReturn(ThreadSendPolicy.NEVER)
        `when`(immutableConfig.logger).thenReturn(NoopLogger)

        `when`(client.getAppDataCollector()).thenReturn(appDataCollector)
        `when`(client.getDeviceDataCollector()).thenReturn(deviceDataCollector)
        `when`(client.getUser()).thenReturn(User("123", "tod@example.com", "Tod"))
        `when`(client.getMetadataState()).thenReturn(MetadataState())
    }

    @Test
    fun getContext() {
        `when`(client.context).thenReturn("Foo")
        assertEquals("Foo", NativeInterface.getContext())
    }

    @Test
    fun getNativeReportPathPersistenceDirectory() {
        val customDir = Files.createTempDirectory("custom").toFile()
        `when`(immutableConfig.persistenceDirectory).thenReturn(lazy { customDir })
        val observed = NativeInterface.getNativeReportPath()
        val expected = File(customDir, "bugsnag/native")
        assertEquals(expected, observed)
    }

    @Test
    fun getUserData() {
        val data = mapOf(Pair("id", "123"), Pair("email", "tod@example.com"), Pair("name", "Tod"))
        assertEquals(data, NativeInterface.getUser())
    }

    @Test
    fun getAppData() {
        `when`(appDataCollector.generateAppWithState()).thenReturn(generateAppWithState())
        `when`(appDataCollector.getAppDataMetadata()).thenReturn(
            mutableMapOf(
                Pair(
                    "metadata",
                    true
                )
            )
        )
        val expected =
            mapOf(Pair("metadata", true), Pair("type", "android"), Pair("versionCode", 0))
        assertEquals(expected, NativeInterface.getApp().filter { it.value != null })
    }

    @Test
    fun getDeviceData() {
        `when`(deviceDataCollector.generateDeviceWithState(anyLong())).thenReturn(
            generateDeviceWithState()
        )
        `when`(deviceDataCollector.getDeviceMetadata()).thenReturn(mapOf(Pair("metadata", true)))
        assertTrue(NativeInterface.getDevice()["metadata"] as Boolean)
    }

    @Test
    fun getCpuAbi() {
        `when`(deviceDataCollector.getCpuAbi()).thenReturn(arrayOf("x86"))
        assertArrayEquals(arrayOf("x86"), NativeInterface.getCpuAbi())
    }

    @Test
    fun getMetadata() {
        val map = mapOf(Pair("Foo", mapOf(Pair("wham", "bar"))))
        `when`(client.metadata).thenReturn(map)
        assertEquals(map, NativeInterface.getMetadata())
    }

    @Test
    fun getBreadcrumbs() {
        val breadcrumbs = listOf(Breadcrumb("Whoops", NoopLogger))
        `when`(client.breadcrumbs).thenReturn(breadcrumbs)
        assertEquals(breadcrumbs[0], NativeInterface.getBreadcrumbs()[0])
    }

    @Test
    fun getReleaseStage() {
        `when`(immutableConfig.releaseStage).thenReturn("dev")
        assertEquals("dev", NativeInterface.getReleaseStage())
    }

    @Test
    fun getSessionEndpoint() {
        assertEquals("http://sessions.bugsnag.com", NativeInterface.getSessionEndpoint())
    }

    @Test
    fun getEndpoint() {
        assertEquals("http://notify.bugsnag.com", NativeInterface.getEndpoint())
    }

    @Test
    fun getAppVersion() {
        `when`(immutableConfig.appVersion).thenReturn("1.2.3")
        assertEquals("1.2.3", NativeInterface.getAppVersion())
    }

    @Test
    fun getEnabledReleaseStages() {
        `when`(immutableConfig.enabledReleaseStages).thenReturn(setOf("prod"))
        assertEquals(setOf("prod"), NativeInterface.getEnabledReleaseStages())
    }

    @Test
    fun getLogger() {
        `when`(immutableConfig.logger).thenReturn(NoopLogger)
        assertEquals(NoopLogger, NativeInterface.getLogger())
    }

    @Test
    fun setUser() {
        NativeInterface.setUser("9", "bob@example.com", "Bob")
        verify(client, times(1)).setUser("9", "bob@example.com", "Bob")
    }

    @Test
    fun leaveBreadcrumb() {
        NativeInterface.leaveBreadcrumb("wow", BreadcrumbType.LOG)
        verify(client, times(1)).leaveBreadcrumb("wow", emptyMap(), BreadcrumbType.LOG)
    }

    @Test
    fun leaveBreadcrumbMetadata() {
        NativeInterface.leaveBreadcrumb("wow", "log", mapOf(Pair("foo", "bar")))
        verify(client, times(1)).leaveBreadcrumb(
            "wow",
            mapOf(Pair("foo", "bar")),
            BreadcrumbType.LOG
        )
    }

    @Test
    fun clearMetadata() {
        NativeInterface.clearMetadata("Foo", "Bar")
        verify(client, times(1)).clearMetadata("Foo", "Bar")
    }

    @Test
    fun addMetadata() {
        NativeInterface.addMetadata("Foo", "Bar", "Baz")
        verify(client, times(1)).addMetadata("Foo", "Bar", "Baz")
    }

    @Test
    fun setContext() {
        NativeInterface.setContext("Foo")
        verify(client, times(1)).context = "Foo"
    }

    @Test
    fun setBinaryArch() {
        NativeInterface.setBinaryArch("x86")
        verify(client, times(1)).setBinaryArch("x86")
    }

    @Test
    fun registerSession() {
        NativeInterface.registerSession(1, "55", 0, 1)
        verify(sessionTracker, times(1)).registerExistingSession(
            any(),
            eq("55"),
            any(),
            eq(0),
            eq(1)
        )
    }

    @Test
    fun deliverReport() {
        NativeInterface.deliverReport(
            null, "{}".toByteArray(), "{}".toByteArray(),
            "", false
        )
        verify(eventStore, times(1)).enqueueContentForDelivery(eq("{}"), any())
    }

    @Test
    fun notifyJVMStackTraceCall() {
        NativeInterface.notify(
            "SIGPIPE",
            "SIGSEGV 11",
            Severity.ERROR,
            arrayOf<StackTraceElement>()
        )
        verify(client, times(1)).notify(any(), any())
    }

    @Test
    fun notifyNativeTraceCall() {
        val nativeStackframe = NativeStackframe(
            "someMethod",
            "libtest.so",
            null,
            98765,
            null,
            null,
            false,
            ErrorType.C,
            "no identifying characteristics"
        )

        NativeInterface.notify("SIGPIPE", "SIGSEGV 11", Severity.ERROR, arrayOf(nativeStackframe))
        verify(client, times(1)).populateAndNotifyAndroidEvent(eventCapture.capture(), any())

        val notifiedEvent = eventCapture.value
        assertNotNull(notifiedEvent)
        assertEquals(1, notifiedEvent.errors.size)

        val error = notifiedEvent.errors.single()
        assertEquals("SIGPIPE", error.errorClass)
        assertEquals("SIGSEGV 11", error.errorMessage)

        assertEquals(1, error.stacktrace.size)
        val stackframe = error.stacktrace.single()
        assertEquals(nativeStackframe.method, stackframe.method)
        assertEquals(nativeStackframe.file, stackframe.file)
        assertEquals(nativeStackframe.frameAddress, stackframe.frameAddress)
        assertEquals(nativeStackframe.type, stackframe.type)
        assertEquals(nativeStackframe.codeIdentifier, stackframe.codeIdentifier)
    }

    @Test
    fun autoDetectAnrs() {
        NativeInterface.setAutoDetectAnrs(true)
        verify(client, times(1)).setAutoDetectAnrs(true)
    }

    @Test
    fun autoNotify() {
        NativeInterface.setAutoNotify(true)
        verify(client, times(1)).setAutoNotify(true)
    }
}
