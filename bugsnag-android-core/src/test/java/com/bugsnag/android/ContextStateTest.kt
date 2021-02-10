package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test

class ContextStateTest {
    @Test
    fun copy() {
        val original = ContextState("foo")
        assertEquals(original.context, original.copy().context)
    }
}
