package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class ThreadStateSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<JsonStream.Streamable, String>> {
            val config = Configuration("api-key")
            val frame = StackTraceElement("Foo.kt", "bar", "Foo.kt", 55)
            val cachedThread = CachedThread(config, 1, "my-thread", "android", true, arrayOf(frame))
            return generateTestCases("thread_state", ThreadState(arrayOf(cachedThread)))
        }
    }

    @Parameter
    lateinit var testCase: Pair<ThreadState, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
