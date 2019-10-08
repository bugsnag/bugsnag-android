package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.io.BufferedReader
import java.io.StringReader

@RunWith(Parameterized::class)
internal class ErrorDeserializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Error, String>> {
            val handledState = HandledState.newInstance(HandledState.REASON_UNHANDLED_EXCEPTION)
            val config = Configuration("api-key")
            val frame = StackTraceElement("Foo.kt", "bar", "Foo.kt", 55)
            val cachedThread = CachedThread(config, 1, "my-thread", "android", true, arrayOf(frame))
            val threadState = ThreadState(arrayOf(cachedThread))

            return generateDeserializationTestCases(
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
    fun testJsonDeserialisation() {
        val reader = JsonParser().parse(testCase.second)
        val err = ErrorReader.readError(Configuration("api-key"), reader)

        val expected = testCase.first
        assertEquals(expected.context, err.context)
        assertEquals(expected.groupingHash, err.groupingHash)
        assertEquals(expected.severity, err.severity)
    }
}
