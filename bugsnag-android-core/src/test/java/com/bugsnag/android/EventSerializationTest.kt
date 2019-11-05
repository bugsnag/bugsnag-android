package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class EventSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Event, String>> {
            val event = Event(
                null,
                generateImmutableConfig(),
                HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION),
                stackTrace = arrayOf()
            )
            event.threads.clear()
            return generateSerializationTestCases("event", event)
        }
    }

    @Parameter
    lateinit var testCase: Pair<Event, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
