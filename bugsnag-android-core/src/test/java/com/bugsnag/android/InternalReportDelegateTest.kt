package com.bugsnag.android

import android.content.Context
import android.os.storage.StorageManager
import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class InternalReportDelegateTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var appData: AppData

    @Mock
    lateinit var deviceData: DeviceData

    @Mock
    lateinit var sessionTracker: SessionTracker

    @Mock
    lateinit var storageManager: StorageManager

    @Test
    fun onErrorIOFailure() {
        val app = HashMap<String, Any>()
        app["foo"] = 2
        `when`(this.appData.appDataSummary).thenReturn(app)
        `when`(this.appData.appData).thenReturn(mapOf(Pair("packageName", "com.example")))
        `when`(this.appData.calculateDurationInForeground()).thenReturn(500)
        `when`(sessionTracker.isInForeground).thenReturn(true)

        val device = HashMap<String, Any>()
        device["id"] = "234"

        `when`(deviceData.deviceDataSummary).thenReturn(device)
        `when`(deviceData.calculateFreeDisk()).thenReturn(10592342221)

        val config = generateImmutableConfig()
        val delegate = InternalReportDelegate(
            context,
            NoopLogger,
            config,
            storageManager,
            appData,
            deviceData,
            sessionTracker
        )

        val handledState = HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION)
        val event = Event(RuntimeException(), config, handledState)
        delegate.reportInternalBugsnagError(event)

        // app
        assertEquals(2, event.app["foo"])
        assertEquals(500L, event.app["durationInForeground"])
        assertEquals(true, event.app["inForeground"])

        // device
        assertEquals("234", event.device["id"])
        assertEquals(10592342221, event.device["freeDisk"])

        // metadata
        assertNotNull(event.getMetadata("BugsnagDiagnostics", "notifierName"))
        assertNotNull(event.getMetadata("BugsnagDiagnostics", "notifierVersion"))
        assertEquals("test", event.getMetadata("BugsnagDiagnostics", "apiKey"))
        assertEquals("com.example", event.getMetadata("BugsnagDiagnostics", "packageName"))
    }
}
