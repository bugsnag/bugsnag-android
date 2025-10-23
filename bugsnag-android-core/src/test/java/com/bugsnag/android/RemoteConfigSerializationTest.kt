package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class RemoteConfigSerializationTest {

    companion object {

        @JvmStatic
        @Parameters
        fun testCases() = generateSerializationTestCases(
            "remoteconfig",
            RemoteConfig(
                "tag123",
                DateUtils.fromIso8601("2024-01-15T10:30:45.123Z"),
                listOf(DiscardRule.All, DiscardRule.AllHandled)
            ),
            RemoteConfig(
                "tag456",
                DateUtils.fromIso8601("2024-01-15T10:30:45.123Z"),
                emptyList()
            )
        )
    }

    @Parameter
    lateinit var testCase: Pair<RemoteConfig, String>

    @Test
    fun testJsonSerialization() = verifyJsonMatches(testCase.first, testCase.second)
}
