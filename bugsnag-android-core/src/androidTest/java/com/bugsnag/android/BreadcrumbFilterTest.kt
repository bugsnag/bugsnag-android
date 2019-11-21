package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateClient
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BreadcrumbFilterTest {

    private var client: Client? = null

    @Before
    fun setUp() {
        val configuration = generateConfiguration()
        configuration.enabledBreadcrumbTypes = setOf(BreadcrumbType.REQUEST)
        client = generateClient(configuration)
    }

    @After
    fun tearDown() {
        client!!.close()
    }

    @Test
    fun zeroBreadcrumbsReceived() {
        client!!.leaveBreadcrumb("Hello")
        assertEquals(0, client!!.breadcrumbState.store.size.toLong())

        client!!.leaveBreadcrumb("Hello", BreadcrumbType.REQUEST, emptyMap())
        assertEquals(1, client!!.breadcrumbState.store.size.toLong())
    }
}
