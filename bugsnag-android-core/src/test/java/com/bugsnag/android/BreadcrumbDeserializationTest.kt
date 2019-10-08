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
internal class BreadcrumbDeserializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases() = generateDeserializationTestCases(
            "breadcrumb",
            Breadcrumb("helloworld", BreadcrumbType.MANUAL, Date(0), mapOf())
        )
    }

    @Parameter
    lateinit var testCase: Pair<Breadcrumb, String>

    @Test
    fun testJsonDeserialisation() {
        val reader = JsonParser().parse(testCase.second)
        val breadcrumb = ErrorReader.readBreadcrumb(reader)

        val expected = testCase.first
        assertEquals(expected.name, breadcrumb.name)
        assertEquals(expected.type, breadcrumb.type)
        assertEquals(expected.timestamp, breadcrumb.timestamp)
        assertEquals(expected.metadata, breadcrumb.metadata)
    }
}
