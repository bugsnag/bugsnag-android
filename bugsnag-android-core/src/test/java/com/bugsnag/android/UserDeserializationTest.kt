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
internal class UserDeserializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases() = generateDeserializationTestCases(
            "user",
            User("123", "bob@example.com", "bob smith")
        )
    }

    @Parameter
    lateinit var testCase: Pair<User, String>

    @Test
    fun testJsonDeserialisation() {
        val reader = JsonParser().parse(testCase.second)
        val user = ErrorReader.readUser(reader)

        val expected = testCase.first
        assertEquals(expected.id, user.id)
        assertEquals(expected.name, user.name)
        assertEquals(expected.email, user.email)
    }
}
