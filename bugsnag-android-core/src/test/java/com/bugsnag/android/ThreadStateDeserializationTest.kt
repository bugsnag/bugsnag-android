package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class ThreadStateDeserializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<ThreadState, String>> {
            val config = Configuration("key")
            val stacktrace = arrayOf(StackTraceElement("Foo.kt", "bar", "Foo.kt", 55))
            val thread = CachedThread(config, 1, "my-thread", "android", true, stacktrace)
            val threadState = ThreadState(arrayOf(thread))
            return generateDeserializationTestCases("thread_state", threadState)
        }
    }

    @Parameter
    lateinit var testCase: Pair<ThreadState, String>

    @Test
    fun testJsonDeserialisation() {
        val reader = JsonParser().parse(testCase.second)
        val threadState = ErrorReader.readThreadState(reader)
        val expected = testCase.first
        assertEquals(expected.cachedThreads[0].name, threadState.cachedThreads[0].name)
    }
}
