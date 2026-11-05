package com.bugsnag.android

import android.app.ApplicationExitInfo
import com.bugsnag.android.test.mockAppExitInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.io.File

/**
 * Upgrading from an SDK version that persisted the exit-info store without a timestamp must not
 * cause every historical exit reason to be discarded.
 */
class ExitInfoPluginStoreLegacyStateTest {
    private lateinit var storeFile: File

    private val previousPid = 4567

    private val exitInfoList = listOf(
        mockAppExitInfo(1234, 1000L, ApplicationExitInfo.REASON_CRASH),
        mockAppExitInfo(previousPid, 2000L, ApplicationExitInfo.REASON_ANR),
    )

    @Before
    fun setUp() {
        storeFile = File.createTempFile("bugsnag-exit-reasons", null)
        storeFile.writeText(previousPid.toString())
    }

    @After
    fun tearDown() {
        storeFile.delete()
    }

    @Test
    fun legacyStateHasUnsetTimestamp() {
        val store = ExitInfoPluginStore(storeFile, mock())
        assertEquals(previousPid, store.previousState?.pid)
        assertEquals(0L, store.previousState?.timestamp)
    }

    @Test
    fun legacyStateDoesNotFilterHistoricalExitInfo() {
        val store = ExitInfoPluginStore(storeFile, mock())
        val filtered = store.previousState!!.filterApplicationExitInfo(exitInfoList)
        assertEquals(exitInfoList, filtered)
    }

    @Test
    fun legacyStateFiltersAlreadyProcessedExitInfo() {
        val state = ExitInfoPluginStore.PersistentState(
            pid = previousPid,
            timestamp = 0L,
            processedExitInfoKeys = setOf(ExitInfoKey(1234, 1000L)),
        )
        val filtered = state.filterApplicationExitInfo(exitInfoList)
        assertEquals(listOf(exitInfoList.last()), filtered)
    }

    @Test
    fun legacyStateSynthesizesAnrFromPreviousRun() {
        val store = ExitInfoPluginStore(storeFile, mock())
        var enhancedEventCount = 0
        val synthesizer = EventSynthesizer(
            createEmptyANRWithTimestamp = { _ -> mock<Event>() },
            anrEventEnhancer = { _, _ -> enhancedEventCount++ },
            exitInfoPluginStore = store,
            reportUnmatchedANRs = true
        )

        val event = synthesizer.createEventWithExitInfo(exitInfoList.last())
        assertNotNull("an ANR from the previous run should be synthesized", event)
        assertEquals(1, enhancedEventCount)
    }
}
