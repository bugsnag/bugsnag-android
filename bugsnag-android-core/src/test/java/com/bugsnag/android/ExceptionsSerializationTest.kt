package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class ExceptionsSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<JsonStream.Streamable, String>> {
            val config = Configuration("api-key")

            // basic exception
            val oops = RuntimeException("oops")
            val exceptions0 = Exceptions(config, BugsnagException(oops))

            // cause exception
            val ex = RuntimeException("oops", Exception("cause"))
            val exceptions1 = Exceptions(config, BugsnagException(ex))

            // named exception
            val element = StackTraceElement("Class", "method", "Class.java", 123)
            val frames = arrayOf(element)
            val error = Error.Builder(
                config, "RuntimeException",
                "Example message", frames, null,
                Thread.currentThread()
            ).build()
            val exceptions2 = Exceptions(config, BugsnagException(error.exception))

            return generateTestCases("exceptions", exceptions0, exceptions1, exceptions2)
        }
    }

    @Parameter
    lateinit var testCase: Pair<Exceptions, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
