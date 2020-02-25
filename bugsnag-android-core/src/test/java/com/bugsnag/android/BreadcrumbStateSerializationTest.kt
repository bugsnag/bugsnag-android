package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.util.Date

@RunWith(Parameterized::class)
internal class BreadcrumbStateSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<JsonStream.Streamable, String>> {
            val breadcrumbs = BreadcrumbState(50, CallbackState(), NoopLogger)
            val metadata = mutableMapOf<String, Any?>(Pair("direction", "left"))
            breadcrumbs.add(Breadcrumb("hello world", BreadcrumbType.MANUAL, metadata, Date(0), NoopLogger))
            return generateSerializationTestCases("breadcrumb_state", breadcrumbs)
        }
    }

    @Parameter
    lateinit var testCase: Pair<BreadcrumbState, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
