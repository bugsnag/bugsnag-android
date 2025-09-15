package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.util.Date

@RunWith(Parameterized::class)
internal class RemoteConfigSerializationTest {

    companion object {

        val allRule: DiscardRule = DiscardRule.All()
        val allHandledRule: DiscardRule = DiscardRule.AllHandled()
        val dateString = "2024-01-15T10:30:45.123Z"
        val date = DateUtils.fromIso8601(dateString)

        @JvmStatic
        @Parameters
        fun testCases() = generateSerializationTestCases(
            "remoteconfig",
            RemoteConfig(
                "tag123",
                date,
                listOf(allRule, allHandledRule)
            )
        )
    }

    @Parameter
    lateinit var testCase: Pair<User, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
