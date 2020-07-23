package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class NotifierSerializationTest {

    companion object {

        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<JsonStream.Streamable, String>> {
            val notifier = Notifier()
            notifier.version = "9.9.9"
            notifier.name = "AndroidBugsnagNotifier"
            notifier.url = "https://bugsnag.com"

            val deps = Notifier()
            deps.version = "4.5.6"
            deps.name = "CustomNotifier"
            deps.url = "https://example.com"
            deps.dependencies = listOf(notifier)
            return generateSerializationTestCases("notifier", notifier, deps)
        }
    }

    @Parameter
    lateinit var testCase: Pair<Notifier, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
