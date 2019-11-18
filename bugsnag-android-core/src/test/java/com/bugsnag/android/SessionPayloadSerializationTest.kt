package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.util.Date

@RunWith(Parameterized::class)
internal class SessionPayloadSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Any, String>> {
            val session = Session("123", Date(0), User(null, null, null), 1, 0)
            Notifier.getInstance().version = "9.9.9"
            Notifier.getInstance().name = "AndroidBugsnagNotifier"
            Notifier.getInstance().url = "https://bugsnag.com"
            return generateSerializationTestCases(
                "session_payload",
                SessionPayload(
                    session,
                    emptyList(),
                    mutableMapOf(),
                    mutableMapOf()
                )
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<SessionPayload, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
