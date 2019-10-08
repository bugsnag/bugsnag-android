package com.bugsnag.android

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class ExceptionsDeserializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Exceptions, String>> {
            val config = Configuration("api-key")
            val frame = StackTraceElement("Foo.kt", "bar", "Foo.kt", 55)
            val exceptions0 = Exceptions(config, BugsnagException("Whoops", "error", arrayOf(frame)))
            return generateSerializationTestCases("exceptions", exceptions0)
        }
    }

    @Parameter
    lateinit var testCase: Pair<Exceptions, String>

    @Test
    fun testJsonDeserialisation() {
        val reader = JsonParser().parse(testCase.second)
        val exc = ErrorReader.readExceptions(Configuration("api-key"), reader)

        val expected = testCase.first
        assertEquals(expected.exceptionType, exc.exceptionType)
        assertArrayEquals(expected.projectPackages, exc.projectPackages)
        assertEquals(expected.exception.name, exc.exception.name)
    }
}
