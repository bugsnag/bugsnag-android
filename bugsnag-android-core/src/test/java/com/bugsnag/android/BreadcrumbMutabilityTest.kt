package com.bugsnag.android

import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Date

class BreadcrumbMutabilityTest {
    @Test
    fun breadcrumbProtectsMetadata() {
        val data = mutableMapOf<String, Any?>()
        val breadcrumb = Breadcrumb("foo", BreadcrumbType.MANUAL, data, Date(0), NoopLogger)
        breadcrumb.metadata!!["a"] = "bar"
        assertFalse(breadcrumb.metadata!!.isEmpty())
    }
}
