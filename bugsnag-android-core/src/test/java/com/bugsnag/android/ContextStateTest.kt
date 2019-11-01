package com.bugsnag.android

import org.junit.Test

import org.junit.Assert.*

class ContextStateTest {
    @Test
    fun copy() {
        val original = ContextState("foo")
        assertEquals(original.context, original.copy().context)
    }
}