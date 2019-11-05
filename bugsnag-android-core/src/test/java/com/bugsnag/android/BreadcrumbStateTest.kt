package com.bugsnag.android

import com.bugsnag.android.BreadcrumbType.MANUAL
import org.junit.Assert.assertEquals

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import java.util.HashMap
import java.util.Locale

class BreadcrumbStateTest {

    private lateinit var breadcrumbState: BreadcrumbState

    @Before
    fun setUp() {
        breadcrumbState = BreadcrumbState(20, NoopLogger)
    }

    /**
     * Verifies that the breadcrumb message is truncated after the max limit is reached
     */
    @Test
    fun testMessageTruncation() {
        breadcrumbState.add(Breadcrumb("Started app"))
        breadcrumbState.add(Breadcrumb("Clicked a button"))
        val longStr = ("Lorem ipsum dolor sit amet, consectetur adipiscing elit,"
                + " sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad "
                + "minim veniam, quis nostrud exercitation ullamco laboris nisi"
                + " ut aliquip ex ea commodo consequat.")
        breadcrumbState.add(Breadcrumb(longStr))

        val crumbs = breadcrumbState.store.toList()
        assertEquals(3, crumbs.size)
        assertEquals(longStr, crumbs[2].metadata["message"])
    }

    /**
     * Verifies that leaving breadcrumbState drops the oldest breadcrumb after reaching the max limit
     */
    @Test
    fun testSizeLimitBeforeAdding() {
        breadcrumbState = BreadcrumbState(5, NoopLogger)

        for (k in 1..6) {
            breadcrumbState.add(Breadcrumb("$k"))
        }

        val crumbs = breadcrumbState.store.toList()
        assertEquals("2", crumbs.first().metadata["message"])
        assertEquals("6", crumbs.last().metadata["message"])
    }

    /**
     * Verifies that no breadcrumbState are added if the size limit is set to 0
     */
    @Test
    fun testSetSizeEmpty() {
        breadcrumbState = BreadcrumbState(0, NoopLogger)
        breadcrumbState.add(Breadcrumb("1"))
        breadcrumbState.add(Breadcrumb("2"))
        assertTrue(breadcrumbState.store.isEmpty())
    }

    /**
     * Verifies that setting a negative size has no effect
     */
    @Test
    fun testSetSizeNegative() {
        breadcrumbState = BreadcrumbState(-1, NoopLogger)
        breadcrumbState.add(Breadcrumb("1"))
        assertEquals(0, breadcrumbState.store.size)
    }

    /**
     * Verifies that clearing removes all the breadcrumbState
     */
    @Test
    fun testClear() {
        breadcrumbState.add(Breadcrumb("1"))
        breadcrumbState.clear()
        assertTrue(breadcrumbState.store.isEmpty())
    }

    /**
     * Verifies that the type of a breadcrumb is manual by default
     */
    @Test
    fun testDefaultBreadcrumbType() {
        breadcrumbState.add(Breadcrumb("1"))
        assertEquals(MANUAL, breadcrumbState.store.peek().type)
    }

    /**
     * Ensures a breadcrumb is dropped if it exceeds the payload size limit
     */
    @Test
    fun testPayloadSizeLimit() {
        val metadata = HashMap<String, Any?>()
        for (i in 0..399) {
            metadata[String.format(Locale.US, "%d", i)] = "!!"
        }
        breadcrumbState.add(Breadcrumb("Rotated Menu", BreadcrumbType.STATE, metadata))
        assertTrue(breadcrumbState.store.isEmpty())
    }

    /**
     * Verifies that the max breadcrumb accessors only allow positive numbers
     */
    @Test
    fun testMaxBreadcrumbAccessors() {
        val config = Configuration("api-key")
        assertEquals(25, config.maxBreadcrumbs)

        config.maxBreadcrumbs = 50
        assertEquals(50, config.maxBreadcrumbs)

        config.maxBreadcrumbs = Int.MAX_VALUE
        assertEquals(100, config.maxBreadcrumbs)

        config.maxBreadcrumbs = -5
        assertEquals(0, config.maxBreadcrumbs)
    }
}
