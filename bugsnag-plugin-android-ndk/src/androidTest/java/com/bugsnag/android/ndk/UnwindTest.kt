package com.bugsnag.android.ndk

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs

class UnwindTest {

    @Test
    fun testUnwindForNotify() {
        assertFramesMatchExpected(
            unwindForNotify(),
            "Java_com_bugsnag_android_ndk_UnwindTest_unwindForNotify",
        )
    }

    @Test
    fun testUnwindForCrash() {
        assertFramesMatchExpected(
            unwindForCrash(),
            "Java_com_bugsnag_android_ndk_UnwindTest_unwindForCrash",
        )
    }

    // find a frame matching a method, returning the index, or fail the test
    fun findIndexByMethod(items: List<Map<String, Any>>, method: String): Int {
        val index = items.indices.find { items[it]["method"] == method }
        if (index == null) {
            fail("did not find method '$method' in list: $items")
        }
        return index!!
    }

    // verify that a stack trace contains the entries in EXPECTED_FUNCTIONS and
    // is followed by the expected JNI function
    fun assertFramesMatchExpected(frames: List<Map<String, Any>>, jniFunction: String) {
        // the top of the stack includes the unwinder itself, etc, so first
        // scan for the expected contents, which must be a sequence
        val offset = findIndexByMethod(frames, EXPECTED_FUNCTIONS[0])

        // check if frames have enough remaining room for expected contents
        assertTrue(
            "missing expected stack frames in $frames",
            frames.size - offset > EXPECTED_FUNCTIONS.size + 1
        )

        EXPECTED_FUNCTIONS.forEachIndexed { index, name ->
            assertFrameMatches(index, name, frames[offset + index])
        }

        assertFrameMatches( // JNI function must immediately follow
            EXPECTED_FUNCTIONS.size,
            jniFunction,
            frames[offset + EXPECTED_FUNCTIONS.size]
        )
    }

    fun assertFrameMatches(frameIndex: Int, expectedFunctionName: String, frame: Map<String, Any>) {
        val method = frame["method"] as String
        val file = frame["file"] as String
        val symbolAddress = frame["symbolAddress"] as Long
        val lineNumber = frame["lineNumber"] as Long
        val loadAddress = frame["loadAddress"] as Long

        assertEquals(
            """
            expected function name '$expectedFunctionName' at index $frameIndex
            actual frame: $frame
            native function addresses: $nativeInfo
            """,
            expectedFunctionName,
            method
        )

        assertTrue(
            """
            expected file suffix '$EXPECTED_NATIVE_LIB_NAME' at index $frameIndex
            actual frame: $frame
            native function addresses: $nativeInfo
            """,
            file.endsWith(EXPECTED_NATIVE_LIB_NAME)
        )

        // these are smol functions. the symbol and return addresses should be
        // close to the local representation, with there being a bit of a larger
        // gap on x86 than the other three ABIs, which are near exact.
        val addressDelta = wordsize * 4

        assertAlmostEquals(
            """
            expected function address to nearly equal ${nativeInfo[expectedFunctionName]} at index $frameIndex
            actual frame: $frame
            native function addresses: $nativeInfo
            """,
            nativeInfo[expectedFunctionName]!!,
            symbolAddress,
            addressDelta
        )

        // sanity check that the line number doesn't include load addr or
        // vice versa
        assertTrue(
            """
            expected line number and load address to be > 0 at index $frameIndex
            actual frame: $frame
            native function addresses: $nativeInfo
            """,
            loadAddress > 0 && lineNumber > 0
        )

        assertAlmostEquals(
            """
            expected line number + load address to nearly equal ${nativeInfo[expectedFunctionName]} at index $frameIndex
            actual frame: $frame
            native function addresses: $nativeInfo
            """,
            nativeInfo[expectedFunctionName]!!,
            lineNumber + loadAddress,
            addressDelta
        )
    }

    // assert two values are equal within an acceptable delta
    fun assertAlmostEquals(message: String, expectedValue: Long, actualValue: Long, delta: Long) {
        val diff = abs(expectedValue - actualValue)
        assertTrue(message + " \ndiff: $diff delta: $delta", diff <= delta)
    }

    // unwind a known set of functions and return the stack frame contents
    external fun unwindForNotify(): List<Map<String, Any>>
    external fun unwindForCrash(): List<Map<String, Any>>

    companion object BuildInfo {
        // these are the names and ordering of the native functions expected to
        // be in the detected stack traces (see UnwindTest.cpp)
        val EXPECTED_FUNCTIONS = listOf(
            "unwind_func_four",
            "unwind_func_three",
            "unwind_func_two",
            "unwind_func_one"
        )

        // name of the library containing the EXPECTED_FUNCTIONS
        const val EXPECTED_NATIVE_LIB_NAME = "libbugsnag-ndk-test.so"

        // get expected frame info. returns a map of frame names to function addresses
        external fun getNativeFunctionInfo(): Map<String, Long>

        val nativeInfo = getNativeFunctionInfo()

        val is64bit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            Build.SUPPORTED_64_BIT_ABIS.size > 0
        else
            false
        val wordsize: Long = if (is64bit) 64 else 32
    }
}
