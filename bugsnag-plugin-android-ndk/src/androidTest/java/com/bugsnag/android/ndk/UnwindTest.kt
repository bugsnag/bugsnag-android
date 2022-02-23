package com.bugsnag.android.ndk

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class UnwindTest {

    @Test
    fun testUnwindForNotify() {
        val frames = unwindForNotify()
        // the top of the stack includes the unwinder itself, etc, so first
        // scan for the expected contents, which must be a sequence
        var offset = frames.indexOf("unwind_func_four")
        if (offset == -1) {
            fail("did not find initial stack frame in list: $frames")
        }
        assertEquals("unwind_func_four", frames[offset])
        assertEquals("unwind_func_three", frames[offset + 1])
        assertEquals("unwind_func_two", frames[offset + 2])
        assertEquals("unwind_func_one", frames[offset + 3])
        assertEquals("Java_com_bugsnag_android_ndk_UnwindTest_unwindForNotify", frames[offset + 4])
    }

    @Test
    fun testUnwindForCrash() {
        val frames = unwindForCrash()
        // the top of the stack includes the unwinder itself, etc, so first
        // scan for the expected contents, which must be a sequence
        var offset = frames.indexOf("unwind_func_four")
        if (offset == -1) {
            fail("did not find initial stack frame in list: $frames")
        }
        assertEquals("unwind_func_four", frames[offset])
        assertEquals("unwind_func_three", frames[offset + 1])
        assertEquals("unwind_func_two", frames[offset + 2])
        assertEquals("unwind_func_one", frames[offset + 3])
        assertEquals("Java_com_bugsnag_android_ndk_UnwindTest_unwindForCrash", frames[offset + 4])
    }

    // unwind a known set of functions and validate the method names in the
    // stack
    external fun unwindForNotify(): List<String>
    external fun unwindForCrash(): List<String>
}
