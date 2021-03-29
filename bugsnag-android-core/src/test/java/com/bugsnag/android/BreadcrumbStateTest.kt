package com.bugsnag.android

import com.bugsnag.android.BreadcrumbType.MANUAL
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        val longStr = (
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit," +
                " sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad " +
                "minim veniam, quis nostrud exercitation ullamco laboris nisi" +
                " ut aliquip ex ea commodo consequat."
            )
        breadcrumbState.add(Breadcrumb(longStr, NoopLogger))

        val crumbs = breadcrumbState.store.toList()
        assertEquals(3, crumbs.size)
        assertEquals(longStr, crumbs[2].message)
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
        assertEquals("2", crumbs.first().message)
        assertEquals("6", crumbs.last().message)
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
        val crumb = requireNotNull(breadcrumbState.store.peek())
        assertEquals(MANUAL, crumb.type)
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
     * Verifies that the max breadcrumb accessors only allow positive numbers
     */
    @Test
    fun testMaxBreadcrumbAccessors() {
        val config = generateConfiguration()
        assertEquals(25, config.maxBreadcrumbs)

        config.maxBreadcrumbs = 50
        assertEquals(50, config.maxBreadcrumbs)

        config.maxBreadcrumbs = Int.MAX_VALUE
        assertEquals(50, config.maxBreadcrumbs)

        config.maxBreadcrumbs = -5
        assertEquals(50, config.maxBreadcrumbs)
    }

    /**
     * Verifies that an [OnBreadcrumbCallback] callback is run when specified in [BreadcrumbState]
     */
    @Test
    fun testOnBreadcrumbCallback() {
        val breadcrumb = Breadcrumb("Whoops", NoopLogger)
        breadcrumbState.callbackState.addOnBreadcrumb(
            OnBreadcrumbCallback {
                true
            }
        )
        breadcrumbState.add(breadcrumb)
        assertEquals(1, breadcrumbState.store.size)
        assertEquals(breadcrumb, breadcrumbState.store.peek())
    }

    /**
     * Verifies that returning false in one callback will halt subsequent callbacks and not apply breadcrumb
     */
    @Test
    fun testOnBreadcrumbCallbackFalse() {
        val requiredBreadcrumb = Breadcrumb("Hello there", NoopLogger)
        breadcrumbState.add(requiredBreadcrumb)

        val breadcrumb = Breadcrumb("Whoops", NoopLogger)
        breadcrumbState.callbackState.addOnBreadcrumb(
            OnBreadcrumbCallback { givenBreadcrumb ->
                givenBreadcrumb.metadata?.put("callback", "first")
                false
            }
        )
        breadcrumbState.callbackState.addOnBreadcrumb(
            OnBreadcrumbCallback { givenBreadcrumb ->
                givenBreadcrumb.metadata?.put("callback", "second")
                true
            }
        )
        breadcrumbState.add(breadcrumb)
        assertEquals(1, breadcrumbState.store.size)
        assertEquals(requiredBreadcrumb, breadcrumbState.store.first())
        assertNotNull(breadcrumb.metadata)
        assertEquals("first", breadcrumb.metadata?.get("callback"))
    }

    /**
     * Verifies that an exception within an OnBreadcrumbCallback allows subsequent callbacks to run
     */
    @Test
    fun testOnBreadcrumbCallbackException() {
        val breadcrumb = Breadcrumb("Whoops", NoopLogger)
        breadcrumbState.callbackState.addOnBreadcrumb(
            OnBreadcrumbCallback {
                throw IllegalStateException("Oh no")
            }
        )
        breadcrumbState.callbackState.addOnBreadcrumb(
            OnBreadcrumbCallback { givenBreadcrumb ->
                givenBreadcrumb.metadata?.put("callback", "second")
                true
            }
        )
        breadcrumbState.add(breadcrumb)
        assertEquals(1, breadcrumbState.store.size)
        assertEquals(breadcrumb, breadcrumbState.store.peek())
        assertNotNull(breadcrumb.metadata)
        assertEquals("second", breadcrumb.metadata?.get("callback"))
    }
}
