package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.io.File

@RunWith(Parameterized::class)
internal class EventPayloadSerializationTest {

    companion object {
        private val notifier = Notifier()
        private val config = BugsnagTestUtils.generateImmutableConfig()

        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<JsonStream.Streamable, String>> {
            notifier.version = "9.9.9"
            notifier.name = "AndroidBugsnagNotifier"
            notifier.url = "https://bugsnag.com"
            return generateSerializationTestCases(
                "report",
                EventPayload("api-key", null, File(""), notifier, config)
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<EventPayload, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
