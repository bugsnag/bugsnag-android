package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class BugsnagExceptionSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<JsonStream.Streamable, String>> {
            val trace = arrayOf(StackTraceElement("Foo", "bar", "Foo.kt", 1))
            val exc = BugsnagException("MyClass", "Custom message", trace)
            return generateSerializationTestCases("bugsnag_exception", exc)
        }
    }

    @Parameter
    lateinit var testCase: Pair<BugsnagException, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
