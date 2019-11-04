package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class HandledStateSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases() = generateSerializationTestCases(
            "handled_state",
            HandledState.newInstance(HandledState.REASON_UNHANDLED_EXCEPTION),
            HandledState.newInstance(HandledState.REASON_STRICT_MODE, Severity.ERROR, "diskRead"),
            HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION),
            HandledState.newInstance(HandledState.REASON_USER_SPECIFIED),
            HandledState.newInstance(HandledState.REASON_CALLBACK_SPECIFIED, Severity.INFO, null),
            HandledState.newInstance(HandledState.REASON_PROMISE_REJECTION),
            HandledState.newInstance(HandledState.REASON_LOG, Severity.WARNING, "warning"),
            HandledState.newInstance(HandledState.REASON_ANR)
        )
    }

    @Parameter
    lateinit var testCase: Pair<HandledState, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
