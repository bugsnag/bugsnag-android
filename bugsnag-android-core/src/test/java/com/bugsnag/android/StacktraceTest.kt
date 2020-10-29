package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test

class StacktraceTest {

    @Test
    fun stackframeLimits() {
        val stackList = mutableListOf<Stackframe>()
        for (i in 1..300) {
            stackList.add(Stackframe("A", "B", i, true))
        }
        val stacktrace = Stacktrace(stackList)
        // Confirm the length of the stackList
        assertEquals(300, stackList.size)
        assertEquals(200, stacktrace.trace.size)
        assertEquals(1, stacktrace.trace.first().lineNumber)
        assertEquals(200, stacktrace.trace.last().lineNumber)
    }
}
