package com.bugsnag.android

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.RuntimeException
import java.lang.Thread
import java.util.Collections

class ThreadStateMissingTraceTest {

    @Test
    fun handleNullThreadTraces() {
        val currentThread = Thread.currentThread()
        val traces = Thread.getAllStackTraces()

        // make all stacktraces null
        traces.keys.forEach {
            traces[it] = null
        }

        val state = ThreadState(
            RuntimeException(),
            false,
            ThreadSendPolicy.ALWAYS,
            Collections.emptyList(),
            NoopLogger,
            currentThread,
            traces
        )
        assertNotNull(state)
        assertTrue(state.threads.isEmpty())
    }
}
