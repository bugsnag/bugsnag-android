package com.bugsnag.android

import android.content.Context
import android.os.storage.StorageManager
import com.bugsnag.android.BugsnagTestUtils.generateDeviceWithState
import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import com.bugsnag.android.BugsnagTestUtils.generateAppWithState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class InternalEventPayloadDelegateTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var appDataCollector: AppDataCollector

    @Mock
    lateinit var deviceDataCollector: DeviceDataCollector

    @Mock
    lateinit var sessionTracker: SessionTracker

    @Mock
    lateinit var storageManager: StorageManager

    @Test
    fun onErrorIOFailure() {
        val app = generateAppWithState()
        `when`(this.appDataCollector.generateAppWithState()).thenReturn(app)
        app.durationInForeground = 500L
        app.inForeground = true
        `when`(deviceDataCollector.generateDeviceWithState(ArgumentMatchers.anyLong())).thenReturn(generateDeviceWithState())

        val config = generateImmutableConfig()
        val delegate = InternalReportDelegate(
            context,
            NoopLogger,
            config,
            storageManager,
            appDataCollector,
            deviceDataCollector,
            sessionTracker,
            Notifier()
        )

        val handledState = HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION)
        val event = Event(RuntimeException(), config, handledState, NoopLogger)
        delegate.reportInternalBugsnagError(event)

        // app
        assertEquals(500L, event.app.durationInForeground)
        assertEquals(true, event.app.inForeground)
        assertNotNull(event.app)

        // device
        assertEquals(22234423124, event.device.freeDisk)

        // metadata
        assertNotNull(event.getMetadata("BugsnagDiagnostics", "notifierName"))
        assertNotNull(event.getMetadata("BugsnagDiagnostics", "notifierVersion"))
        assertEquals("5d1ec5bd39a74caa1267142706a7fb21", event.getMetadata("BugsnagDiagnostics", "apiKey"))
    }
}
