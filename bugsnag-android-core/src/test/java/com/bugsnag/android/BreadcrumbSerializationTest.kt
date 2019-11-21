package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.util.Date

@RunWith(Parameterized::class)
internal class BreadcrumbSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Breadcrumb, String>> {
            val timestamp = Date(0)
            return generateSerializationTestCases(
                "breadcrumb",
                Breadcrumb("hello world", BreadcrumbType.MANUAL, mutableMapOf(), timestamp),
                Breadcrumb(
                    "metadata",
                    BreadcrumbType.PROCESS,
                    mutableMapOf(Pair("foo", true)),
                    timestamp
                )
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<Breadcrumb, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
