package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class SeverityReasonSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<SeverityReason, String>> {
            val override = SeverityReason.newInstance(
                SeverityReason.REASON_HANDLED_EXCEPTION
            )
            override.unhandled = true

            return generateSerializationTestCases(
                "severity_reason",
                SeverityReason.newInstance(SeverityReason.REASON_UNHANDLED_EXCEPTION),
                SeverityReason.newInstance(SeverityReason.REASON_STRICT_MODE, Severity.ERROR, "diskRead"),
                SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
                SeverityReason.newInstance(SeverityReason.REASON_USER_SPECIFIED),
                SeverityReason.newInstance(SeverityReason.REASON_CALLBACK_SPECIFIED, Severity.INFO, null),
                SeverityReason.newInstance(SeverityReason.REASON_PROMISE_REJECTION),
                SeverityReason.newInstance(SeverityReason.REASON_LOG, Severity.WARNING, "warning"),
                SeverityReason.newInstance(SeverityReason.REASON_ANR),
                override,
                SeverityReason.newInstance(SeverityReason.REASON_HANDLED_ERROR)
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<SeverityReason, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
