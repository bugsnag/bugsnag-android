package com.bugsnag.android

import com.bugsnag.android.Thread.ThreadSendPolicy
import com.bugsnag.android.Thread.ThreadSendPolicy.ALWAYS
import com.bugsnag.android.Thread.ThreadSendPolicy.NEVER
import com.bugsnag.android.Thread.ThreadSendPolicy.UNHANDLED_ONLY
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadSendPolicyTest {

    @Test
    fun invalidFromString() {
        assertEquals(ALWAYS, ThreadSendPolicy.fromString(""))
        assertEquals(ALWAYS, ThreadSendPolicy.fromString("foo"))
    }

    @Test
    fun validFromString() {
        assertEquals(ALWAYS, ThreadSendPolicy.fromString("ALWAYS"))
        assertEquals(NEVER, ThreadSendPolicy.fromString("NEVER"))
        assertEquals(UNHANDLED_ONLY, ThreadSendPolicy.fromString("UNHANDLED_ONLY"))
    }
}
