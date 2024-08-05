package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test

class StacktraceTest {

    @Test
    fun stackframeListTrimmed() {
        val stackList = (1..300).mapTo(ArrayList()) { index ->
            Stackframe("A", "B", index, true)
        }
        val stacktrace = Stacktrace(stackList)
        // Confirm the length of the stackList
        assertEquals(300, stackList.size)
        assertEquals(200, stacktrace.trace.size)
        assertEquals(1, stacktrace.trace.first().lineNumber)
        assertEquals(200, stacktrace.trace.last().lineNumber)
    }

    @Test
    fun stacktraceElementArrayTrimmed() {
        val trace = (1..300).map { index ->
            StackTraceElement("A", "B", "C", index)
        }.toTypedArray()

        val stacktrace = Stacktrace(trace, emptyList(), NoopLogger)
        // Confirm the length of the stackList
        assertEquals(300, trace.size)
        assertEquals(200, stacktrace.trace.size)
        assertEquals(1, stacktrace.trace.first().lineNumber)
        assertEquals(200, stacktrace.trace.last().lineNumber)
    }
}
