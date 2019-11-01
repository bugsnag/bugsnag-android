package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class ReportSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<JsonStream.Streamable, String>> {
            Notifier.version = "9.9.9"
            Notifier.name = "AndroidBugsnagNotifier"
            Notifier.url = "https://bugsnag.com"
            return generateSerializationTestCases(
                "report",
                Report("api-key", null)
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<Report, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
