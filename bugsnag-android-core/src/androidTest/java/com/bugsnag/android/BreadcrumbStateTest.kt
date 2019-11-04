package com.bugsnag.android

import com.bugsnag.android.BreadcrumbType.MANUAL
import org.junit.Assert.assertEquals

import androidx.test.filters.SmallTest
import com.bugsnag.android.BugsnagTestUtils.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date

@SmallTest
class BreadcrumbStateTest {

    private lateinit var breadcrumbState: BreadcrumbState
    private var client: Client? = null

    @Before
    fun setUp() {
        breadcrumbState = BreadcrumbState(20, NoopLogger)
        client = generateClient()
    }

    @After
    fun tearDown() {
        client?.close()
    }

    /**
     * Verifies that the Client methods leave breadcrumbState correctly
     */
    @Test
    fun testClientMethods() {
        client!!.leaveBreadcrumb("Hello World")
        val store = client!!.breadcrumbState.store
        var count = 0

        for (breadcrumb in store) {
            if (MANUAL == breadcrumb.type && "manual" == breadcrumb.message
                && breadcrumb.metadata["message"] == "Hello World") {
                count++
            }
        }
        assertEquals(1, count)
    }

}
