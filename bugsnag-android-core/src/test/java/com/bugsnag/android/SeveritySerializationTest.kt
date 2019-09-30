package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class SeveritySerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases() = generateTestCases(
            "severity",
            Severity.ERROR,
            Severity.WARNING,
            Severity.INFO
        )
    }

    @Parameter
    lateinit var testCase: Pair<User, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
