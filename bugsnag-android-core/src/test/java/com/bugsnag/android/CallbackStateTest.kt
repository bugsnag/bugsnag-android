package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CallbackStateTest {

    @Mock
    lateinit var app: App

    @Mock
    lateinit var device: Device

    @Mock
    lateinit var session: Session

    private val handledState = SeverityReason.newInstance(
        SeverityReason.REASON_HANDLED_EXCEPTION
    )
    private val event =
        Event(RuntimeException(), generateImmutableConfig(), handledState, NoopLogger)
    private val breadcrumb = Breadcrumb("", NoopLogger)

    @Test
    fun testCopy() {
        val state = CallbackState()
        state.addOnError(OnErrorCallback { true })
        state.addOnBreadcrumb(OnBreadcrumbCallback { true })
        state.addOnSession(OnSessionCallback { true })
        state.addOnSend(OnSendCallback { true })

        val copy = state.copy()
        assertEquals(1, copy.onErrorTasks.size)
        assertEquals(1, copy.onBreadcrumbTasks.size)
        assertEquals(1, copy.onSessionTasks.size)
        assertEquals(1, copy.onSendTasks.size)
    }

    @Test
    fun onErrorExcThrown() {
        val state = CallbackState()
        state.addOnError(OnErrorCallback { true })
        state.addOnError(OnErrorCallback { throw IllegalArgumentException() })

        val logger = InterceptingLogger()
        assertNull(logger.msg)
        assertTrue(state.runOnErrorTasks(event, logger))
        assertNotNull(logger.msg)
    }

    @Test
    fun onErrorFalseReturned() {
        val state = CallbackState()
        var count = 0
        state.addOnError(OnErrorCallback { false })
        state.addOnError(
            OnErrorCallback {
                count = 1
                true
            }
        )
        assertFalse(state.runOnErrorTasks(event, NoopLogger))
        assertEquals(0, count)
    }

    @Test
    fun onSessionExcThrown() {
        val state = CallbackState()
        state.addOnSession(OnSessionCallback { true })
        state.addOnSession(OnSessionCallback { throw IllegalArgumentException() })

        val logger = InterceptingLogger()
        assertNull(logger.msg)
        assertTrue(state.runOnSessionTasks(session, logger))
        assertNotNull(logger.msg)
    }

    @Test
    fun onSessionFalseReturned() {
        val state = CallbackState()
        var count = 0
        state.addOnSession(OnSessionCallback { false })
        state.addOnSession(
            OnSessionCallback {
                count = 1
                true
            }
        )
        assertFalse(state.runOnSessionTasks(session, NoopLogger))
        assertEquals(0, count)
    }

    @Test
    fun onBreadcrumbExcThrown() {
        val state = CallbackState()
        state.addOnBreadcrumb(OnBreadcrumbCallback { true })
        state.addOnBreadcrumb(OnBreadcrumbCallback { throw IllegalArgumentException() })

        val logger = InterceptingLogger()
        assertNull(logger.msg)
        assertTrue(state.runOnBreadcrumbTasks(breadcrumb, logger))
        assertNotNull(logger.msg)
    }

    @Test
    fun onBreadcrumbFalseReturned() {
        val state = CallbackState()
        var count = 0
        state.addOnBreadcrumb(OnBreadcrumbCallback { false })
        state.addOnBreadcrumb(
            OnBreadcrumbCallback {
                count = 1
                true
            }
        )
        assertFalse(state.runOnBreadcrumbTasks(breadcrumb, NoopLogger))
        assertEquals(0, count)
    }

    @Test
    fun onSendExcThrown() {
        val state = CallbackState()
        state.addOnSend(OnSendCallback { true })
        state.addOnSend(OnSendCallback { throw IllegalArgumentException() })

        val logger = InterceptingLogger()
        assertNull(logger.msg)
        assertTrue(state.runOnSendTasks({ event }, logger))
        assertNotNull(logger.msg)
    }

    @Test
    fun onSendFalseReturned() {
        val state = CallbackState()
        var count = 0
        state.addOnSend(OnSendCallback { false })
        state.addOnSend(
            OnSendCallback {
                count = 1
                true
            }
        )
        assertFalse(state.runOnSendTasks({ event }, NoopLogger))
        assertEquals(0, count)
    }

    @Test
    fun testOnPreSend() {
        val state = CallbackState()
        state.addOnSend(OnSendCallback { true })
        val onSendCallBack = state.onSendTasks.first()
        state.addPreOnSend(OnSendCallback { true })
        val expectedOnSendCallBackPosition = state.onSendTasks.indexOf(onSendCallBack)

        assertEquals(2, state.onSendTasks.size)
        assertEquals(1, expectedOnSendCallBackPosition)
    }
}
