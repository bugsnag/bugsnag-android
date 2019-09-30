package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.util.Date

@RunWith(Parameterized::class)
internal class BreadcrumbsSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<JsonStream.Streamable, String>> {
            val breadcrumbs = Breadcrumbs(Configuration("api-key"))
            breadcrumbs.add(Breadcrumb("hello world", BreadcrumbType.MANUAL, Date(0), mapOf()))
            return generateTestCases("breadcrumbs", breadcrumbs)
        }
    }

    @Parameter
    lateinit var testCase: Pair<Breadcrumbs, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
