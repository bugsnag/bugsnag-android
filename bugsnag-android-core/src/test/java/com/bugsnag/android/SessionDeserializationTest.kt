package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.io.BufferedReader
import java.io.StringReader
import java.util.Date

@RunWith(Parameterized::class)
internal class SessionDeserializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases() = generateDeserializationTestCases(
            "session",
            Session("123", Date(0), User(), 0, 0)
        )
    }

    @Parameter
    lateinit var testCase: Pair<Session, String>

    @Test
    fun testJsonDeserialisation() {
        val reader = JsonParser().parse(testCase.second)
        val session = ErrorReader.readSession(reader)

        val expected = testCase.first
        assertEquals(expected.id, session.id)
        assertEquals(expected.handledCount, session.handledCount)
        assertEquals(expected.unhandledCount, session.unhandledCount)
        assertEquals(expected.startedAt, session.startedAt)
    }
}
