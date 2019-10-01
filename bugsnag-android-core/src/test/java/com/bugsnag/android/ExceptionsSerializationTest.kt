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
            val frame = StackTraceElement("Foo.kt", "bar", "Foo.kt", 55)
            val exceptions0 = Exceptions(config, BugsnagException("Whoops", "error", arrayOf(frame)))
            return generateTestCases("exceptions", exceptions0)
        }
    }

    @Parameter
    lateinit var testCase: Pair<Exceptions, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
