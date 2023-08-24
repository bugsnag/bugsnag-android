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
    private lateinit var eventEnhancer: (Event, ApplicationExitInfo) -> Unit

    @Mock
    private lateinit var event: Event

    @Mock
    private lateinit var exitInfo1: ApplicationExitInfo

    @Mock
    private lateinit var session: Session

    @Mock
    private lateinit var am: ActivityManager

    private var exitInfos = listOf<ApplicationExitInfo>()

    @Before
    fun setUp() {
        exitInfoCallback = ExitInfoCallback(context, eventEnhancer)
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
        verify(eventEnhancer, times(0)).invoke(event, exitInfo1)
    }

    @Test
    fun testProcessStateSummaryIsMatch() {
        `when`(exitInfos.first().processStateSummary).thenReturn("1".toByteArray())
        `when`(event.session?.id).thenReturn("1")
        assertTrue(exitInfoCallback.onSend(event))
        verify(eventEnhancer, times(1)).invoke(event, exitInfo1)
    }

    @Test
    fun testProcessStateSummaryIsNotMatch() {
        `when`(exitInfos.first().processStateSummary).thenReturn("1".toByteArray())
        `when`(event.session?.id).thenReturn("test")
        assertTrue(exitInfoCallback.onSend(event))
        verify(eventEnhancer, times(0)).invoke(event, exitInfo1)
    }
}
