package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.util.Date

@RunWith(Parameterized::class)
internal class SessionSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases() = generateSerializationTestCases(
            "session",
            Session("123", Date(0), User(null, null, null), 1, 0)
        )
    }

    @Parameter
    lateinit var testCase: Pair<Session, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
