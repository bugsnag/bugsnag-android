package com.bugsnag.android

import com.bugsnag.android.BreadcrumbType.MANUAL
import org.junit.Assert.assertEquals

import androidx.test.filters.SmallTest
import com.bugsnag.android.BugsnagTestUtils.*

import org.json.JSONException
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.io.IOException
import java.util.HashMap
import java.util.Locale

@SmallTest
class BreadcrumbsTest {

    private lateinit var breadcrumbs: Breadcrumbs
    private var client: Client? = null

    @Before
    fun setUp() {
        breadcrumbs = Breadcrumbs(32)
        client = generateClient()
    }

    @After
    fun tearDown() {
        client?.close()
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

        val breadcrumbsJson = streamableToJsonArray(breadcrumbs)
        assertEquals(3, breadcrumbsJson.length())
        assertEquals("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do "
            + "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim",
            breadcrumbsJson.getJSONObject(2).getJSONObject("metaData").get("message"))
    }

    /**
     * Verifies that leaving breadcrumbs drops the oldest breadcrumb after reaching the max limit
     */
    @Test
    @Throws(JSONException::class, IOException::class)
    fun testSizeLimitBeforeAdding() {
        breadcrumbs = Breadcrumbs(5)

        for (k in 1..6) {
            breadcrumbs.add(Breadcrumb("$k"))
        }

        val json = streamableToJsonArray(breadcrumbs)
        assertEquals(5, json.length())

        val firstCrumb = json.getJSONObject(0)
        val lastCrumb = json.getJSONObject(4)
        assertEquals("2", firstCrumb.getJSONObject("metaData").get("message"))
        assertEquals("6", lastCrumb.getJSONObject("metaData").get("message"))
    }

    /**
     * Verifies that no breadcrumbs are added if the size limit is set to 0
     */
    @Test
    fun testSetSizeEmpty() {
        breadcrumbs = Breadcrumbs(0)
        breadcrumbs.add(Breadcrumb("1"))
        breadcrumbs.add(Breadcrumb("2"))
        breadcrumbs.add(Breadcrumb("3"))
        assertEquals(0, streamableToJsonArray(breadcrumbs).length())
    }

    /**
     * Verifies that setting a negative size has no effect
     */
    @Test
    fun testSetSizeNegative() {
        breadcrumbs = Breadcrumbs(-1)
        breadcrumbs.add(Breadcrumb("1"))
        assertEquals(0, streamableToJsonArray(breadcrumbs).length())
    }

    /**
     * Verifies that clearing removes all the breadcrumbs
     */
    @Test
    fun testClear() {
        breadcrumbs.add(Breadcrumb("1"))
        breadcrumbs.clear()
        assertEquals(0, streamableToJsonArray(breadcrumbs).length())
    }

    /**
     * Verifies that the type of a breadcrumb is manual by default
     */
    @Test
    fun testDefaultBreadcrumbType() {
        breadcrumbs.add(Breadcrumb("1"))
        val json = streamableToJsonArray(breadcrumbs)
        assertEquals("manual", json.getJSONObject(0).get("type"))
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
        assertEquals(0, streamableToJsonArray(breadcrumbs).length())
    }

    /**
     * Verifies that breadcrumb metadata is serialised
     */
    @Test
    fun testPayloadType() {
        val metadata = mapOf(Pair("direction", "left"))
        breadcrumbs.add(Breadcrumb("Rotated Menu", BreadcrumbType.STATE, metadata))

        val json = streamableToJsonArray(breadcrumbs)
        val node = json.getJSONObject(0)
        assertEquals("Rotated Menu", node.get("name"))
        assertEquals("state", node.get("type"))
        assertEquals("left", node.getJSONObject("metaData").get("direction"))
        assertEquals(1, json.length())
    }

    /**
     * Verifies that the Client methods leave breadcrumbs correctly
     */
    @Test
    fun testClientMethods() {
        client!!.leaveBreadcrumb("Hello World")
        val store = client!!.breadcrumbs.store
        var count = 0

        for (breadcrumb in store) {
            if (MANUAL == breadcrumb.type && "manual" == breadcrumb.name) {
                count++
                assertEquals("Hello World", breadcrumb.metadata["message"])
            }
        }
        assertEquals(1, count)
    }

    /**
     * Verifies that the max breadcrumb accessors only allow positive numbers
     */
    @Test
    fun testMaxBreadcrumbAccessors() {
        val config = generateConfiguration()
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
