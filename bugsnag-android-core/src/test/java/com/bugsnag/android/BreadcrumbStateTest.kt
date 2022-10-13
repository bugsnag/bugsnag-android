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
        breadcrumbState = createBreadcrumbState(CallbackState())
    }

    private fun createBreadcrumbState(cbState: CallbackState): BreadcrumbState {
        return BreadcrumbState(
            20,
            cbState,
            NoopLogger
        )
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

        val crumbs = breadcrumbState.copy()
        assertEquals(3, crumbs.size)
        assertEquals(longStr, crumbs[2].message)
    }

    /**
     * Verifies that no breadcrumbs are added if the size limit is set to 0
     */
    @Test
    fun testZeroMaxBreadcrumbs() {
        breadcrumbState = BreadcrumbState(0, CallbackState(), NoopLogger)
        breadcrumbState.add(Breadcrumb("1", NoopLogger))
        breadcrumbState.add(Breadcrumb("2", NoopLogger))
        assertTrue(breadcrumbState.copy().isEmpty())
    }

    /**
     * Verifies that one breadcrumb is added if the size limit is set to 1
     */
    @Test
    fun testOneMaxBreadcrumb() {
        breadcrumbState = BreadcrumbState(1, CallbackState(), NoopLogger)
        assertTrue(breadcrumbState.copy().isEmpty())

        breadcrumbState.add(Breadcrumb("A", NoopLogger))
        assertEquals(1, breadcrumbState.copy().size)
        assertEquals("A", breadcrumbState.copy()[0].message)

        breadcrumbState.add(Breadcrumb("B", NoopLogger))
        assertEquals(1, breadcrumbState.copy().size)
        assertEquals("B", breadcrumbState.copy()[0].message)

        breadcrumbState.add(Breadcrumb("C", NoopLogger))
        assertEquals(1, breadcrumbState.copy().size)
        assertEquals("C", breadcrumbState.copy()[0].message)
    }

    /**
     * Verifies that the ring buffer can be filled up to the maxBreadcrumbs limit
     */
    @Test
    fun testRingBufferFilled() {
        breadcrumbState = BreadcrumbState(5, CallbackState(), NoopLogger)

        for (k in 1..5) {
            breadcrumbState.add(Breadcrumb("$k", NoopLogger))
        }

        val crumbs = breadcrumbState.copy()
        assertEquals(5, crumbs.size)
        assertEquals("1", crumbs[0].message)
        assertEquals("2", crumbs[1].message)
        assertEquals("3", crumbs[2].message)
        assertEquals("4", crumbs[3].message)
        assertEquals("5", crumbs[4].message)
    }

    /**
     * Verifies that the breadcrumbs are in order after the ring buffer wraps around by one
     */
    @Test
    fun testRingBufferExceededByOne() {
        breadcrumbState = BreadcrumbState(5, CallbackState(), NoopLogger)

        for (k in 1..6) {
            breadcrumbState.add(Breadcrumb("$k", NoopLogger))
        }

        val crumbs = breadcrumbState.copy()
        assertEquals(5, crumbs.size)
        assertEquals("2", crumbs[0].message)
        assertEquals("3", crumbs[1].message)
        assertEquals("4", crumbs[2].message)
        assertEquals("5", crumbs[3].message)
        assertEquals("6", crumbs[4].message)
    }

    /**
     * Verifies that the breadcrumbs are in order after the ring buffer wraps around by four
     */
    @Test
    fun testRingBufferExceededByFour() {
        breadcrumbState = BreadcrumbState(5, CallbackState(), NoopLogger)

        for (k in 1..9) {
            breadcrumbState.add(Breadcrumb("$k", NoopLogger))
        }

        val crumbs = breadcrumbState.copy()
        assertEquals(5, crumbs.size)
        assertEquals("5", crumbs[0].message)
        assertEquals("6", crumbs[1].message)
        assertEquals("7", crumbs[2].message)
        assertEquals("8", crumbs[3].message)
        assertEquals("9", crumbs[4].message)
    }

    /**
     * Verifies that the breadcrumbs are in order after the ring buffer is filled twice
     */
    @Test
    fun testRingBufferFilledTwice() {
        breadcrumbState = BreadcrumbState(3, CallbackState(), NoopLogger)

        for (k in 1..6) {
            breadcrumbState.add(Breadcrumb("$k", NoopLogger))
        }

        val crumbs = breadcrumbState.copy()
        assertEquals(3, crumbs.size)
        assertEquals("4", crumbs[0].message)
        assertEquals("5", crumbs[1].message)
        assertEquals("6", crumbs[2].message)
    }

    /**
     * Verifies that the type of a breadcrumb is manual by default
     */
    @Test
    fun testDefaultBreadcrumbType() {
        breadcrumbState.add(Breadcrumb("1", NoopLogger))
        val crumb = breadcrumbState.copy()[0]
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
        breadcrumbState.add(
            Breadcrumb(
                "Rotated Menu",
                BreadcrumbType.STATE,
                metadata,
                Date(0),
                NoopLogger
            )
        )
        assertFalse(breadcrumbState.copy().isEmpty())
    }

    /**
     * Verifies that the max breadcrumb accessors only allow positive numbers
     */
    @Test
    fun testMaxBreadcrumbAccessors() {
        val config = generateConfiguration()
        assertEquals(100, config.maxBreadcrumbs)

        config.maxBreadcrumbs = 25
        assertEquals(25, config.maxBreadcrumbs)

        config.maxBreadcrumbs = Int.MAX_VALUE
        assertEquals(25, config.maxBreadcrumbs)

        config.maxBreadcrumbs = -5
        assertEquals(25, config.maxBreadcrumbs)
    }

    /**
     * Verifies that an [OnBreadcrumbCallback] callback is run when specified in [BreadcrumbState]
     */
    @Test
    fun testOnBreadcrumbCallback() {
        val breadcrumb = Breadcrumb("Whoops", NoopLogger)
        val cbState = CallbackState(
            onBreadcrumbTasks = mutableListOf(
                OnBreadcrumbCallback {
                    true
                }
            )
        )
        breadcrumbState = createBreadcrumbState(cbState)
        breadcrumbState.add(breadcrumb)
        val copy = breadcrumbState.copy()
        assertEquals(1, copy.size)
        assertEquals(breadcrumb, copy.first())
    }

    /**
     * Verifies that returning false in one callback will halt subsequent callbacks and not apply breadcrumb
     */
    @Test
    fun testOnBreadcrumbCallbackFalse() {
        val cbState = CallbackState()
        breadcrumbState = createBreadcrumbState(cbState)

        val requiredBreadcrumb = Breadcrumb("Hello there", NoopLogger)
        breadcrumbState.add(requiredBreadcrumb)

        cbState.addOnBreadcrumb(
            OnBreadcrumbCallback { givenBreadcrumb ->
                givenBreadcrumb.metadata?.put("callback", "first")
                false
            }
        )
        cbState.addOnBreadcrumb(
            OnBreadcrumbCallback { givenBreadcrumb ->
                givenBreadcrumb.metadata?.put("callback", "second")
                true
            }
        )

        val breadcrumb = Breadcrumb("Whoops", NoopLogger)
        breadcrumbState.add(breadcrumb)

        val copy = breadcrumbState.copy()
        assertEquals(1, copy.size)
        assertEquals(requiredBreadcrumb, copy.first())
        assertNotNull(breadcrumb.metadata)
        assertEquals("first", breadcrumb.metadata?.get("callback"))
    }

    /**
     * Verifies that an exception within an OnBreadcrumbCallback allows subsequent callbacks to run
     */
    @Test
    fun testOnBreadcrumbCallbackException() {
        val cbState = CallbackState(
            onBreadcrumbTasks = mutableListOf(
                OnBreadcrumbCallback {
                    throw IllegalStateException("Oh no")
                },
                OnBreadcrumbCallback { givenBreadcrumb ->
                    givenBreadcrumb.metadata?.put("callback", "second")
                    true
                }
            )
        )
        breadcrumbState = createBreadcrumbState(cbState)

        val breadcrumb = Breadcrumb("Whoops", NoopLogger)
        breadcrumbState.add(breadcrumb)

        val copy = breadcrumbState.copy()
        assertEquals(1, copy.size)
        assertEquals(breadcrumb, copy[0])
        assertNotNull(breadcrumb.metadata)
        assertEquals("second", breadcrumb.metadata?.get("callback"))
    }

    @Test
    fun testCopyThenAdd() {
        breadcrumbState = BreadcrumbState(25, CallbackState(), NoopLogger)

        repeat(1000) { count ->
            breadcrumbState.add(Breadcrumb("$count", NoopLogger))
        }

        assertEquals(25, breadcrumbState.copy().size)
    }
}
