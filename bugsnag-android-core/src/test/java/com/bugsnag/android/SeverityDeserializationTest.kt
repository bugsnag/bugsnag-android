package com.bugsnag.android

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class SeverityDeserializationTest {

    companion object {
        @JvmStatic
        @Parameters
            fun testCases() = generateDeserializationTestCases(
            "severity",
            Severity.ERROR,
            Severity.WARNING,
            Severity.INFO
        )
    }

    @Parameter
    lateinit var testCase: Pair<Severity, String>

    @Test
    fun testJsonDeserialisation() {
        val reader = JsonParser().parse(testCase.second)
        val severity = ErrorReader.readSeverity(reader)
        Assert.assertEquals(testCase.first, severity)
    }
}
