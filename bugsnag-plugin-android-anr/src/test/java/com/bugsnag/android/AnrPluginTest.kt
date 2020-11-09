package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test

class AnrPluginTest {
    private fun newNativeFrame(): StackTraceElement {
        return StackTraceElement("com.bugsnag.testing.SomeClass", "someNativeMethod", "afile", -2)
    }
    private fun newAnrDetectedFrame(): StackTraceElement {
        return StackTraceElement("com.bugsnag.android.AnrPlugin", "notifyAnrDetected", "afile", 1)
    }

    @Test
    fun testNativeAnrIndexWithNativeStack() {
        val frames = arrayOf<StackTraceElement>(
            StackTraceElement("aclass", "amethod", "afile", 1),
            newAnrDetectedFrame(),
            newNativeFrame(),
            StackTraceElement("class2", "method2", "afile", 1)
        )
        val index = AnrPlugin.getNativeANRIndex(frames)
        assertEquals(2, index)
    }

    @Test
    fun testNativeAnrIndexWithJavaStack() {
        val frames = arrayOf<StackTraceElement>(
            StackTraceElement("aclass", "amethod", "afile", 1),
            newAnrDetectedFrame(),
            StackTraceElement("class2", "method2", "afile", 1),
            StackTraceElement("class3", "method3", "afile", 1)
        )
        val index = AnrPlugin.getNativeANRIndex(frames)
        assertEquals(-1, index)
    }
}
