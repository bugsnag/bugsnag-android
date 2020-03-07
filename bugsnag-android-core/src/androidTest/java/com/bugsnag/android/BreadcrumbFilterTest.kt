package com.bugsnag.android

import com.bugsnag.android.BreadcrumbType.MANUAL
import org.junit.Assert.assertEquals

import androidx.test.filters.SmallTest
import com.bugsnag.android.BugsnagTestUtils.generateClient

import org.junit.After
import org.junit.Test

@SmallTest
class BreadcrumbFilterTest {

    private var client: Client? = null

    @After
    fun tearDown() {
        client?.close()
    }

    @Test
    fun testManualBreadcrumbNotFiltered() {
        val configuration = BugsnagTestUtils.generateConfiguration()
        configuration.enabledBreadcrumbTypes = emptySet()
        client = generateClient(configuration)

        client?.leaveBreadcrumb("Hello World")

        assertEquals(1, client!!.breadcrumbState.store.size)
    }

    @Test
    fun testManualBreadcrumbNotFilteredOnManual() {
        val configuration = BugsnagTestUtils.generateConfiguration()
        configuration.enabledBreadcrumbTypes = setOf(MANUAL)
        client = generateClient(configuration)

        client?.leaveBreadcrumb("Hello World")

        assertEquals(1, client!!.breadcrumbState.store.size)
    }

}


