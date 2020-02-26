package com.bugsnag.android

import com.bugsnag.android.BreadcrumbType.MANUAL
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

import java.util.HashMap
import java.util.Locale

class BreadcrumbStateTest {

    private lateinit var breadcrumbState: BreadcrumbState

    @Before
    fun setUp() {
        breadcrumbState = BreadcrumbState(20, CallbackState(), NoopLogger)
    }

    /**
     * Verifies that the breadcrumb message is truncated after the max limit is reached
     */
    @Test
    fun testMessageTruncation() {
        breadcrumbState.add(Breadcrumb("Started app", NoopLogger))
        breadcrumbState.add(Breadcrumb("Clicked a button", NoopLogger))
        val longStr = ("Lorem ipsum dolor sit amet, consectetur adipiscing elit,"
                + " sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad "
                + "minim veniam, quis nostrud exercitation ullamco laboris nisi"
                + " ut aliquip ex ea commodo consequat.")
        breadcrumbState.add(Breadcrumb(longStr, NoopLogger))

        val crumbs = breadcrumbState.store.toList()
        assertEquals(3, crumbs.size)
        assertEquals(longStr, crumbs[2].metadata!!["message"])
    }

    /**
     * Verifies that leaving breadcrumbs drops the oldest breadcrumb after reaching the max limit
     */
    @Test
    fun testSizeLimitBeforeAdding() {
        breadcrumbState = BreadcrumbState(5, CallbackState(), NoopLogger)

        for (k in 1..6) {
            breadcrumbState.add(Breadcrumb("$k", NoopLogger))
        }

        val crumbs = breadcrumbState.store.toList()
        assertEquals("2", crumbs.first().metadata!!["message"])
        assertEquals("6", crumbs.last().metadata!!["message"])
    }

    /**
     * Verifies that no breadcrumbs are added if the size limit is set to 0
     */
    @Test
    fun testSetSizeEmpty() {
        breadcrumbState = BreadcrumbState(0, CallbackState(), NoopLogger)
        breadcrumbState.add(Breadcrumb("1", NoopLogger))
        breadcrumbState.add(Breadcrumb("2", NoopLogger))
        assertTrue(breadcrumbState.store.isEmpty())
    }

    /**
     * Verifies that setting a negative size has no effect
     */
    @Test
    fun testSetSizeNegative() {
        breadcrumbState = BreadcrumbState(-1, CallbackState(), NoopLogger)
        breadcrumbState.add(Breadcrumb("1", NoopLogger))
        assertEquals(0, breadcrumbState.store.size)
    }

    /**
     * Verifies that the type of a breadcrumb is manual by default
     */
    @Test
    fun testDefaultBreadcrumbType() {
        breadcrumbState.add(Breadcrumb("1", NoopLogger))
        assertEquals(MANUAL, breadcrumbState.store.peek().type)
    }

    /**
     * Ensures a breadcrumb is not dropped if it contains a large amount of metadata
     */
    @Test
    fun testPayloadSizeLimit() {
        val metadata = HashMap<String, Any?>()
        for (i in 0..399) {
            metadata[String.format(Locale.US, "%d", i)] = "!!"
        }
        breadcrumbState.add(Breadcrumb("Rotated Menu", BreadcrumbType.STATE, metadata, Date(0), NoopLogger))
        assertFalse(breadcrumbState.store.isEmpty())
    }

    /**
     * Verifies that an [OnBreadcrumbCallback] callback is run when specified in [BreadcrumbState]
     */
    @Test
    fun testOnBreadcrumbCallback() {
        breadcrumbState.callbackState.addOnBreadcrumb(OnBreadcrumbCallback { false })
        breadcrumbState.add(Breadcrumb("Whoops", NoopLogger))
        assertTrue(breadcrumbState.store.isEmpty())
    }
}
