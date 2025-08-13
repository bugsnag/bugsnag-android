package com.bugsnag.android

import android.app.ApplicationExitInfo
import com.bugsnag.android.test.mockAppExitInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.File

class SynthesizeUnmatchedANREventsTest {
    private lateinit var storeFile: File
    private lateinit var store: ExitInfoPluginStore

    private val exitInfoList = listOf(
        mockAppExitInfo(1234, 1000L, ApplicationExitInfo.REASON_ANR),
        mockAppExitInfo(2345, 2000L, ApplicationExitInfo.REASON_CRASH),
        mockAppExitInfo(3456, 3000L, ApplicationExitInfo.REASON_ANR),
        mockAppExitInfo(4567, 4000L, ApplicationExitInfo.REASON_ANR),
    )

    @Before
    fun setUp() {
        storeFile = File.createTempFile("bugsnag-exit-reasons", null)
        store = ExitInfoPluginStore(storeFile, mock())
    }

    @After
    fun tearDown() {
        storeFile.delete()
    }

    @Test
    fun smokeTest() {
        var enhancedEventCount = 0
        val synthesizer = EventSynthesizer(
            createEmptyANRWithTimestamp = { _ -> mock<Event>() },
            anrEventEnhancer = { _, _ -> enhancedEventCount++ },
            exitInfoPluginStore = store,
            reportUnmatchedANRs = true
        )

        store.previousState = ExitInfoPluginStore.PersistentState(
            pid = 4567,
            timestamp = 3500L,
            processedExitInfoKeys = emptySet()
        )

        store.currentState = ExitInfoPluginStore.PersistentState(
            pid = 9876,
            timestamp = 5000L,
            processedExitInfoKeys = emptySet()
        )

        val event = synthesizer.createEventWithExitInfo(exitInfoList.last())
        assertNotNull("EventSynthesizer should synthesise an ANR event", event)
        assertEquals("EventSynthesizer should have processed in ANR", 1, enhancedEventCount)

        val secondEventAttempt = synthesizer.createEventWithExitInfo(exitInfoList.last())
        assertNull("ExitInfo should only be synthesized once", secondEventAttempt)
    }

    @Test
    fun shouldNotSynthesizeForProcessedExitInfo() {
        store.previousState = ExitInfoPluginStore.PersistentState(
            pid = 9876,
            timestamp = 5000L,
            processedExitInfoKeys = emptySet()
        )

        store.currentState = ExitInfoPluginStore.PersistentState(
            pid = 6789,
            timestamp = 6000L,
            processedExitInfoKeys = emptySet()
        )

        val synthesizer = EventSynthesizer(
            createEmptyANRWithTimestamp = { _ -> mock<Event>() },
            anrEventEnhancer = { _, _ -> },
            exitInfoPluginStore = store,
            reportUnmatchedANRs = true
        )

        exitInfoList.forEach { exitInfo ->
            assertNull(
                "EventSynthesizer should not synthesize an event for any processed exit info",
                synthesizer.createEventWithExitInfo(exitInfo)
            )
        }
    }

    @Test
    fun shouldOnlyMatchPreviousProcess() {
        var enhancedEventCount = 0
        val synthesizer = EventSynthesizer(
            createEmptyANRWithTimestamp = { _ -> mock<Event>() },
            anrEventEnhancer = { _, _ -> enhancedEventCount++ },
            exitInfoPluginStore = store,
            reportUnmatchedANRs = true
        )

        // somehow the clock went backwards
        store.previousState = ExitInfoPluginStore.PersistentState(
            pid = 5678,
            timestamp = 3500L,
            processedExitInfoKeys = emptySet()
        )

        store.currentState = ExitInfoPluginStore.PersistentState(
            pid = 9876,
            timestamp = 5000L,
            processedExitInfoKeys = emptySet()
        )

        exitInfoList.forEach { exitInfo ->
            assertNull(
                "EventSynthesizer should not synthesize an event for any processed exit info",
                synthesizer.createEventWithExitInfo(exitInfo)
            )
        }
    }
}
