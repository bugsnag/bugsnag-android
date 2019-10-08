package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.util.Date

@RunWith(Parameterized::class)
internal class BreadcrumbsDeserializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Breadcrumbs, String>> {
            val breadcrumbs = Breadcrumbs(Configuration("api-key"))
            breadcrumbs.add(Breadcrumb("helloworld", BreadcrumbType.MANUAL, Date(0), mapOf()))
            return generateDeserializationTestCases("breadcrumbs", breadcrumbs)
        }
    }

    @Parameter
    lateinit var testCase: Pair<Breadcrumbs, String>

    @Test
    fun testJsonDeserialisation() {
        val reader = JsonParser().parse(testCase.second)
        val breadcrumbs = ErrorReader.readBreadcrumbs(Configuration("api-key"), reader)
        val breadcrumb = breadcrumbs.store.poll()
        val expected = testCase.first.store.poll()

        assertEquals(expected.name, breadcrumb.name)
        assertEquals(expected.type, breadcrumb.type)
        assertEquals(expected.timestamp, breadcrumb.timestamp)
        assertEquals(expected.metadata, breadcrumb.metadata)
    }
}
