package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class CachedThreadSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<JsonStream.Streamable, String>> {
            val config = Configuration("key")

            val stacktrace = arrayOf(
                StackTraceElement("", "run_func", "librunner.so", 5038),
                StackTraceElement("Runner", "runFunc", "Runner.java", 14),
                StackTraceElement("App", "launch", "App.java", 70)
            )

            val thread = CachedThread(config, 24, "main-one", "ando", true, stacktrace)

            val stacktrace1 = arrayOf(
                StackTraceElement("", "run_func", "librunner.so", 5038),
                StackTraceElement("Runner", "runFunc", "Runner.java", 14),
                StackTraceElement("App", "launch", "App.java", 70)
            )

            val thread1 = CachedThread(config, 24, "main-one", "ando", false, stacktrace1)
            return generateSerializationTestCases("cached_thread", thread, thread1)
        }
    }

    @Parameter
    lateinit var testCase: Pair<CachedThread, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
