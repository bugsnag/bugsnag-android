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
                Breadcrumb("hello world", BreadcrumbType.MANUAL, mutableMapOf(), timestamp, NoopLogger),
                Breadcrumb(
                    "metadata",
                    BreadcrumbType.PROCESS,
                    mutableMapOf<String, Any?>(
                        "foo" to true,
                        "password" to "super secret"
                    ),
                    timestamp,
                    NoopLogger
                )
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<Breadcrumb, String>

    private val eventMapper = BugsnagEventMapper(NoopLogger)

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)

    @Test
    fun testJsonDeserialization() {
        verifyJsonParser(testCase.first, testCase.second) {
            eventMapper.convertBreadcrumbInternal(it)
        }
    }
}
