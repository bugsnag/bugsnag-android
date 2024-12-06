package com.bugsnag.android

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class ExitInfoCallbackTest {

    private lateinit var exitInfoCallback: ExitInfoCallback

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var nativeEnhancer: (Event, ApplicationExitInfo) -> Unit

    @Mock
    private lateinit var event: Event

    @Mock
    private lateinit var exitInfo1: ApplicationExitInfo

    @Mock
    private lateinit var session: Session

    @Mock
    private lateinit var am: ActivityManager

    private var exitInfos = listOf<ApplicationExitInfo>()

    @Mock
    private lateinit var anrEventEnhancer: (Event, ApplicationExitInfo) -> Unit

    @Before
    fun setUp() {
        exitInfoCallback = ExitInfoCallback(context, nativeEnhancer, anrEventEnhancer, null, ApplicationExitInfoMatcher(context, 100))
        exitInfos = listOf(exitInfo1)
        `when`(context.getSystemService(any())).thenReturn(am)
        `when`(am.getHistoricalProcessExitReasons(any(), anyInt(), anyInt()))
            .thenReturn(exitInfos)
        `when`(event.session).thenReturn(session)
    }

    @Test
    fun testSessionIsNull() {
        event.session = null
        assertTrue(exitInfoCallback.onSend(event))
        verify(nativeEnhancer, times(0)).invoke(event, exitInfo1)
        verify(anrEventEnhancer, times(0)).invoke(event, exitInfo1)
    }

    @Test
    fun testProcessStateSummaryIsMatch() {
        `when`(exitInfos.first().processStateSummary).thenReturn("1".toByteArray())
        `when`(event.session?.id).thenReturn("1")
        assertTrue(exitInfoCallback.onSend(event))
        verify(nativeEnhancer, times(0)).invoke(event, exitInfo1)
        verify(anrEventEnhancer, times(0)).invoke(event, exitInfo1)
    }

    @Test
    fun testProcessStateSummaryIsNotMatch() {
        `when`(exitInfos.first().processStateSummary).thenReturn("1".toByteArray())
        `when`(event.session?.id).thenReturn("test")
        assertTrue(exitInfoCallback.onSend(event))
        verify(nativeEnhancer, times(0)).invoke(event, exitInfo1)
        verify(anrEventEnhancer, times(0)).invoke(event, exitInfo1)
    }

    @Test
    fun testUseTombstoneEnhancer() {
        `when`(exitInfos.first().processStateSummary).thenReturn("1".toByteArray())
        `when`(event.session?.id).thenReturn("1")
        `when`(exitInfo1.reason).thenReturn(ApplicationExitInfo.REASON_CRASH_NATIVE)
        assertTrue(exitInfoCallback.onSend(event))
        verify(nativeEnhancer, times(1)).invoke(event, exitInfo1)
        verify(anrEventEnhancer, times(0)).invoke(event, exitInfo1)
    }

    @Test
    fun testUseTraceEnhancer() {
        `when`(exitInfos.first().processStateSummary).thenReturn("1".toByteArray())
        `when`(event.session?.id).thenReturn("1")
        `when`(exitInfo1.reason).thenReturn(ApplicationExitInfo.REASON_ANR)
        assertTrue(exitInfoCallback.onSend(event))
        verify(nativeEnhancer, times(0)).invoke(event, exitInfo1)
        verify(anrEventEnhancer, times(1)).invoke(event, exitInfo1)
    }

    @Test
    fun testInvalidExitInfo() {
        `when`(exitInfos.first().processStateSummary).thenReturn("1".toByteArray())
        `when`(event.session?.id).thenReturn("1")
        `when`(exitInfo1.reason).thenReturn(ApplicationExitInfo.CONTENTS_FILE_DESCRIPTOR)
        assertTrue(exitInfoCallback.onSend(event))
        verify(nativeEnhancer, times(0)).invoke(event, exitInfo1)
        verify(anrEventEnhancer, times(0)).invoke(event, exitInfo1)
    }

    @Test
    fun testUnknownExitReasonAndImportance() {
        `when`(exitInfos.first().processStateSummary).thenReturn("1".toByteArray())
        `when`(event.session?.id).thenReturn("1")
        `when`(exitInfo1.reason).thenReturn(ApplicationExitInfo.REASON_UNKNOWN)
        `when`(exitInfo1.importance).thenReturn(ActivityManager.RunningAppProcessInfo.REASON_UNKNOWN)
        assertTrue(exitInfoCallback.onSend(event))
        verify(event, times(1)).addMetadata("app", "exitReason", "unknown reason (0)")
        verify(event, times(1)).addMetadata("app", "processImportance", "unknown importance (0)")
    }
}
