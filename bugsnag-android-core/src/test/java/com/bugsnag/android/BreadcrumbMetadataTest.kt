package com.bugsnag.android

import org.junit.Test
import java.math.BigInteger
import java.util.Date

class BreadcrumbMetadataTest {

    @Test
    fun checkComplexTypesRemoved() {
        val crumb = Breadcrumb("hello world", BreadcrumbType.MANUAL, mutableMapOf(), Date(0))
        crumb.metadata["bool"] = true
        crumb.metadata["string"] = "Some data"
        crumb.metadata["int"] = 55
        crumb.metadata["double"] = 3950.20
        crumb.metadata["long"] = 39501098235092342
        crumb.metadata["obj"] = Endpoints()
        crumb.metadata["map"] = mapOf(Pair("foo", "bar"))
        verifyJsonMatches(crumb, "breadcrumb_metadata.json")
    }
}
