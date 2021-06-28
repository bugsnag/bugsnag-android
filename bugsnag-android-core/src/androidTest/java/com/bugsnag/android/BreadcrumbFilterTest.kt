package com.bugsnag.android

import androidx.test.filters.SmallTest
import com.bugsnag.android.BreadcrumbType.MANUAL
import com.bugsnag.android.BugsnagTestUtils.generateClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

@SmallTest
class BreadcrumbFilterTest {

    lateinit var client: Client

    @After
    fun tearDown() {
        client.close()
    }

    @Test
    fun testManualBreadcrumbNotFiltered() {
        val configuration = BugsnagTestUtils.generateConfiguration()
        configuration.enabledBreadcrumbTypes = emptySet()
        client = generateClient(configuration)

        client.leaveBreadcrumb("Hello World")
        assertEquals(1, client.breadcrumbState.copy().size)
    }

    @Test
    fun testManualBreadcrumbNotFilteredOnManual() {
        val configuration = BugsnagTestUtils.generateConfiguration()
        configuration.enabledBreadcrumbTypes = setOf(MANUAL)
        client = generateClient(configuration)

        client.leaveBreadcrumb("Hello World")

        assertEquals(1, client.breadcrumbState.copy().size)
    }
}
