package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.ArrayList
import java.util.HashMap

class CallbackStateTest {

    private val handledState = HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION)
    private val event = Event(RuntimeException(), config = generateImmutableConfig(), handledState = handledState)
    private val breadcrumb = Breadcrumb("")
    private val sessionPayload = SessionPayload(null, ArrayList(), HashMap(), HashMap())

    @Test
    fun testCopy() {
        val state = CallbackState()
        state.addOnError(OnError { true })
        state.addOnBreadcrumb(OnBreadcrumb { true })
        state.addOnSession(OnSession { true })

        val copy = state.copy()
        assertEquals(1, copy.onErrorTasks.size)
        assertEquals(1, copy.onBreadcrumbTasks.size)
        assertEquals(1, copy.onSessionTasks.size)
    }

    @Test
    fun onErrorExcThrown() {
        val state = CallbackState()
        state.addOnError(OnError { true })
        state.addOnError(OnError { throw RuntimeException() })

        val logger = InterceptingLogger()
        assertNull(logger.msg)
        assertTrue(state.runOnErrorTasks(event, logger))
        assertNotNull(logger.msg)
    }

    @Test
    fun onErrorFalseReturned() {
        val state = CallbackState()
        var count = 0
        state.addOnError(OnError { false })
        state.addOnError(OnError {
            count = 1
            true
        })
        assertFalse(state.runOnErrorTasks(event, NoopLogger))
        assertEquals(0, count)
    }

    @Test
    fun onSessionExcThrown() {
        val state = CallbackState()
        state.addOnSession(OnSession { true })
        state.addOnSession(OnSession { throw RuntimeException() })

        val logger = InterceptingLogger()
        assertNull(logger.msg)
        assertTrue(state.runOnSessionTasks(sessionPayload, logger))
        assertNotNull(logger.msg)
    }

    @Test
    fun onSessionFalseReturned() {
        val state = CallbackState()
        var count = 0
        state.addOnSession(OnSession { false })
        state.addOnSession(OnSession {
            count = 1
            true
        })
        assertFalse(state.runOnSessionTasks(sessionPayload, NoopLogger))
        assertEquals(0, count)
    }

    @Test
    fun onBreadcrumbExcThrown() {
        val state = CallbackState()
        state.addOnBreadcrumb(OnBreadcrumb { true })
        state.addOnBreadcrumb(OnBreadcrumb { throw RuntimeException() })

        val logger = InterceptingLogger()
        assertNull(logger.msg)
        assertTrue(state.runOnBreadcrumbTasks(breadcrumb, logger))
        assertNotNull(logger.msg)
    }

    @Test
    fun onBreadcrumbFalseReturned() {
        val state = CallbackState()
        var count = 0
        state.addOnBreadcrumb(OnBreadcrumb { false })
        state.addOnBreadcrumb(OnBreadcrumb {
            count = 1
            true
        })
        assertFalse(state.runOnBreadcrumbTasks(breadcrumb, NoopLogger))
        assertEquals(0, count)
    }

    private class InterceptingLogger: Logger {
        var msg: String? = null
        override fun w(msg: String, throwable: Throwable) {
            this.msg = msg
        }
    }

}
