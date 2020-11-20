package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class StackframeSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Stackframe, String>> {
            val frame = Stackframe("foo", "Bar", 55, true)
            frame.type = ErrorType.ANDROID
            return generateSerializationTestCases(
                "stackframe",
                frame,
                Stackframe(NativeStackframe("aMethod", "aFile", 1, 2, 3, 4)),
                Stackframe(NativeStackframe("aMethod", "aFile", 1, null, null, null)),
                Stackframe(NativeStackframe(null, null, null, null, null, null))
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<Stackframe, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
