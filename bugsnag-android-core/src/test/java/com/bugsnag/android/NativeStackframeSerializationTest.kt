package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class NativeStackframeSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<NativeStackframe, String>> {
            val last = NativeStackframe(null, null, null, null, null, null, null)
            last.type = ErrorType.ANDROID
            val first = NativeStackframe("aMethod", "aFile", 1, 2, 3, 4, true)
            first.type = ErrorType.C
            return generateSerializationTestCases(
                "native_stackframe",
                first,
                NativeStackframe("aMethod", "aFile", 1, null, null, null, null),
                last
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<Stackframe, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
