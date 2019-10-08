package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class CachedThreadDeserializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<CachedThread, String>> {
            val config = Configuration("key")

            val stacktrace = arrayOf(
                StackTraceElement("", "run_func", "librunner.so", 5038),
                StackTraceElement("Runner", "runFunc", "Runner.java", 14),
                StackTraceElement("App", "launch", "App.java", 70)
            )

            val thread = CachedThread(config, 24, "main-one", "ando", true, stacktrace)
            return generateDeserializationTestCases("cached_thread", thread)
        }
    }

    @Parameter
    lateinit var testCase: Pair<CachedThread, String>

    @Test
    fun testJsonDeserialisation() {
        val reader = JsonParser().parse(testCase.second)
        val cachedThread = ErrorReader.readThread(reader)

        val expected = testCase.first
        assertEquals(expected.id, cachedThread.id)
        assertEquals(expected.isErrorReportingThread, cachedThread.isErrorReportingThread)
        assertEquals(expected.name, cachedThread.name)
        assertEquals(expected.type, cachedThread.type)
    }
}
