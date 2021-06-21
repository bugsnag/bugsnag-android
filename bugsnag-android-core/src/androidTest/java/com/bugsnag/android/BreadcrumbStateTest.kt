package com.bugsnag.android

import androidx.test.filters.SmallTest
import com.bugsnag.android.BreadcrumbType.MANUAL
import com.bugsnag.android.BugsnagTestUtils.generateClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@SmallTest
class BreadcrumbStateTest {

    private lateinit var breadcrumbState: BreadcrumbState
    private var client: Client? = null

    @Before
    fun setUp() {
        breadcrumbState = BreadcrumbState(20, CallbackState(), NoopLogger)
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
        client = generateClient()
        client!!.leaveBreadcrumb("Hello World")
        val store = client!!.breadcrumbState.copy()
        var count = 0

        for (breadcrumb in store) {
            if (MANUAL == breadcrumb.type && "Hello World" == breadcrumb.message) {
                count++
            }
        }
        assertEquals(1, count)
    }
}
