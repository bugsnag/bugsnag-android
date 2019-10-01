package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class StacktraceSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases() = generateSerializationTestCases("stacktrace", Stacktrace(arrayOf(), arrayOf()))
    }

    @Parameter
    lateinit var testCase: Pair<Stacktrace, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
