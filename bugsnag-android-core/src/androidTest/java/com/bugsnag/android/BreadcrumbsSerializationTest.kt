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
class BreadcrumbsSerializationTest {

    private lateinit var breadcrumbs: Breadcrumbs
    private var client: Client? = null

    @Before
    fun setUp() {
        breadcrumbs = Breadcrumbs(20)
        client = generateClient()
    }

    @After
    fun tearDown() {
        client?.close()
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

}
