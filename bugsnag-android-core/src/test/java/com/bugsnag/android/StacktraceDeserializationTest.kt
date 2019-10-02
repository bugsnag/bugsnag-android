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
internal class StacktraceDeserializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases() = generateDeserializationTestCases("stacktrace", listOf<Map<String, Any>>())
    }

    @Parameter
    lateinit var testCase: Pair<List<Map<String, Any>>, String>

    @Test
    fun testJsonDeserialisation() {
        val reader = JsonParser().parse(testCase.second)
        val stacktrace = ErrorReader.readStackFrames(reader)

        val expected = testCase.first
        assertEquals(expected, stacktrace)
    }
}
