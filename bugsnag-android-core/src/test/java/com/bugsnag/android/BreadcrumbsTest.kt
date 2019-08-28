package com.bugsnag.android

import com.bugsnag.android.BreadcrumbType.MANUAL
import org.junit.Assert.assertEquals

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import java.util.HashMap
import java.util.Locale

class BreadcrumbsTest {

    private lateinit var breadcrumbs: Breadcrumbs
    private lateinit var config: Configuration

    @Before
    fun setUp() {
        config = Configuration("api-key")
        breadcrumbs = Breadcrumbs(config)
    }

    /**
     * Verifies that the breadcrumb message is truncated after the max limit is reached
     */
    @Test
    fun testMessageTruncation() {
        breadcrumbs.add(Breadcrumb("Started app"))
        breadcrumbs.add(Breadcrumb("Clicked a button"))
        breadcrumbs.add(Breadcrumb("Lorem ipsum dolor sit amet, consectetur adipiscing elit,"
            + " sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad "
            + "minim veniam, quis nostrud exercitation ullamco laboris nisi"
            + " ut aliquip ex ea commodo consequat."))


        val crumbs = breadcrumbs.store.toList()
        assertEquals(3, crumbs.size)
        assertEquals("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do "
            + "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim",
            crumbs[2].metadata["message"])
    }

    /**
     * Verifies that leaving breadcrumbs drops the oldest breadcrumb after reaching the max limit
     */
    @Test
    fun testSizeLimitBeforeAdding() {
        config.maxBreadcrumbs = 5

        for (k in 1..6) {
            breadcrumbs.add(Breadcrumb("$k"))
        }

        val crumbs = breadcrumbs.store.toList()
        assertEquals(config.maxBreadcrumbs, crumbs.size)
        assertEquals("2", crumbs.first().metadata["message"])
        assertEquals("6", crumbs.last().metadata["message"])
    }

    /**
     * Verifies that no breadcrumbs are added if the size limit is set to 0
     */
    @Test
    fun testSetSizeEmpty() {
        config.maxBreadcrumbs = 0
        breadcrumbs.add(Breadcrumb("1"))
        breadcrumbs.add(Breadcrumb("2"))
        assertTrue(breadcrumbs.store.isEmpty())
    }

    /**
     * Verifies that setting a negative size has no effect
     */
    @Test
    fun testSetSizeNegative() {
        config.maxBreadcrumbs = -1
        breadcrumbs.add(Breadcrumb("1"))
        assertEquals(1, breadcrumbs.store.size)
    }

    /**
     * Verifies that clearing removes all the breadcrumbs
     */
    @Test
    fun testClear() {
        breadcrumbs.add(Breadcrumb("1"))
        breadcrumbs.clear()
        assertTrue(breadcrumbs.store.isEmpty())
    }

    /**
     * Verifies that the type of a breadcrumb is manual by default
     */
    @Test
    fun testDefaultBreadcrumbType() {
        breadcrumbs.add(Breadcrumb("1"))
        assertEquals(MANUAL, breadcrumbs.store.peek().type)
    }

    /**
     * Ensures a breadcrumb is dropped if it exceeds the payload size limit
     */
    @Test
    fun testPayloadSizeLimit() {
        val metadata = HashMap<String, String>()
        for (i in 0..399) {
            metadata[String.format(Locale.US, "%d", i)] = "!!"
        }
        breadcrumbs.add(Breadcrumb("Rotated Menu", BreadcrumbType.STATE, metadata))
        assertTrue(breadcrumbs.store.isEmpty())
    }

    /**
     * Verifies that the max breadcrumb accessors only allow positive numbers
     */
    @Test
    fun testMaxBreadcrumbAccessors() {
        assertEquals(32, config.maxBreadcrumbs)

        config.maxBreadcrumbs = 50
        assertEquals(50, config.maxBreadcrumbs)

        config.maxBreadcrumbs = Int.MAX_VALUE
        assertEquals(Int.MAX_VALUE, config.maxBreadcrumbs)

        config.maxBreadcrumbs = 0
        assertEquals(0, config.maxBreadcrumbs)

        config.maxBreadcrumbs = -5
        assertEquals(0, config.maxBreadcrumbs)
    }
}
