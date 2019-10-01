package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.util.Date

@RunWith(Parameterized::class)
internal class SessionTrackingPayloadSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Any, String>> {
            val session = Session("123", Date(0), User(), 1, 0)
            val notifier = Notifier.getInstance()
            notifier.version = "9.9.9"

            return generateTestCases(
                "session_tracking_payload",
                SessionTrackingPayload(
                    session,
                    emptyList(),
                    emptyMap(),
                    emptyMap(),
                    notifier
                )
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<SessionTrackingPayload, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
