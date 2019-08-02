package com.bugsnag.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BreadcrumbMutabilityTest {

    @Test
    fun breadcrumbCopiesMap() {
        val data = mutableMapOf<String, String>()
        val breadcrumb = Breadcrumb("foo", BreadcrumbType.MANUAL, data)
        data["a"] = "bar"
        assertTrue(breadcrumb.metadata.isEmpty())
        assertNotSame(data, breadcrumb.metadata)
    }

    @Test
    fun breadcrumbProtectsMetaData() {
        val data = mutableMapOf<String, String>()
        val breadcrumb = Breadcrumb("foo", BreadcrumbType.MANUAL, data)
        breadcrumb.metadata["a"] = "bar"
        assertFalse(breadcrumb.metadata.isEmpty())
    }
}
