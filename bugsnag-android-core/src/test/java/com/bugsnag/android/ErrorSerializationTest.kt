package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class ErrorSerializationTest {

    companion object {

        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Error, String>> {
            val handledState = HandledState.newInstance(HandledState.REASON_UNHANDLED_EXCEPTION)
            val config = Configuration("api-key")
            val frame = StackTraceElement("Foo.kt", "bar", "Foo.kt", 55)
            val cachedThread = CachedThread(config, 1, "my-thread", "android", true, arrayOf(frame))
            val threadState = ThreadState(arrayOf(cachedThread))

            return generateTestCases(
                "error",
                Error(
                    config,
                    BugsnagException("Whoops", "error", arrayOf(frame)),
                    handledState,
                    Severity.ERROR,
                    null,
                    threadState
                )
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<Error, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
