package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class BugsnagExceptionDeserializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<BugsnagException, String>> {
            val trace = arrayOf(StackTraceElement("Foo", "bar", "Foo.kt", 1))
            val exc = BugsnagException("MyClass", "Custom message", trace)
            return generateDeserializationTestCases("bugsnag_exception", exc)
        }
    }

    @Parameter
    lateinit var testCase: Pair<BugsnagException, String>

    @Test
    fun testJsonDeserialisation() {
        val reader = JsonParser().parse(testCase.second)
        val exc = ErrorReader.readException(reader)

        val expected = testCase.first
        assertEquals(expected.name, exc.name)
        assertEquals(expected.message, exc.message)
        assertEquals(expected.type, exc.type)
    }
}
