package com.bugsnag.android

import org.junit.Assert.assertFalse
import org.junit.Test

class BreadcrumbMutabilityTest {
    @Test
    fun breadcrumbProtectsMetadata() {
        val data = mutableMapOf<String, Any?>()
        val breadcrumb = Breadcrumb("foo", BreadcrumbType.MANUAL, data)
        breadcrumb.metadata["a"] = "bar"
        assertFalse(breadcrumb.metadata.isEmpty())
    }
}
