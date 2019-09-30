package com.bugsnag.android

import com.bugsnag.android.BreadcrumbType.MANUAL
import org.junit.Assert.assertEquals

import androidx.test.filters.SmallTest
import com.bugsnag.android.BugsnagTestUtils.*

import org.junit.After
import org.junit.Before
import org.junit.Test

@SmallTest
class BreadcrumbsClientTest {

    private lateinit var breadcrumbs: Breadcrumbs
    private lateinit var config: Configuration
    private var client: Client? = null

    @Before
    fun setUp() {
        config = generateConfiguration()
        breadcrumbs = Breadcrumbs(config)
        client = generateClient()
    }

    @After
    fun tearDown() {
        client?.close()
    }

    /**
     * Verifies that the Client methods leave breadcrumbs correctly
     */
    @Test
    fun testClientMethods() {
        client!!.leaveBreadcrumb("Hello World")
        val store = client!!.breadcrumbs.store
        var count = 0

        for (breadcrumb in store) {
            if (MANUAL == breadcrumb.type && "manual" == breadcrumb.name) {
                count++
                assertEquals("Hello World", breadcrumb.metadata["message"])
            }
        }
        assertEquals(1, count)
    }

}
