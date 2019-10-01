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
        fun testCases() = generateTestCases(
            "handled_state",
            HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION)
        )
    }

    @Parameter
    lateinit var testCase: Pair<HandledState, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
