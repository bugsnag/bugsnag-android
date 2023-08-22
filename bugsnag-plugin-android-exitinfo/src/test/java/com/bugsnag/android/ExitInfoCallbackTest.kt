package com.bugsnag.android

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class ExitInfoCallbackTest {

    lateinit var exitInfoCallback: ExitInfoCallback

    @Mock
    lateinit var context: Context

    private val eventEnhancer: (Event, ApplicationExitInfo) -> Unit = { _, _ -> }

    @Mock
    lateinit var event: Event

    @Mock
    lateinit var exitInfo1: ApplicationExitInfo

    @Mock
    lateinit var session: Session

    @Mock
    lateinit var am: ActivityManager

    private var exitInfos = listOf<ApplicationExitInfo>()

    @Before
    fun setUp() {
        exitInfoCallback = ExitInfoCallback(context, eventEnhancer)
        exitInfos = listOf(exitInfo1)
        `when`(context.getSystemService(any())).thenReturn(am)
        `when`(am.getHistoricalProcessExitReasons(context.packageName, 0, 100))
            .thenReturn(exitInfos)
        `when`(event.session).thenReturn(session)
    }

    @Test
    fun testBuildVersionLevel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            assertTrue(exitInfoCallback.onSend(event))
        } else {
            assertTrue(exitInfoCallback.onSend(event))
        }
    }

    @Test
    fun testSessionIsNull() {
        event.session = null
        assertTrue(exitInfoCallback.onSend(event))
    }

    @Test
    fun testProcessStateSummaryIsMatch() {
        `when`(exitInfos.first().processStateSummary).thenReturn("1".toByteArray())
        `when`(event.session?.id).thenReturn("1")
        assertTrue(exitInfoCallback.onSend(event))
    }

    @Test
    fun testProcessStateSummaryIsNotMatch() {
        `when`(exitInfos.first().processStateSummary).thenReturn("1".toByteArray())
        `when`(event.session?.id).thenReturn("test")
        assertTrue(exitInfoCallback.onSend(event))
    }
}
