package com.bugsnag.android

import com.bugsnag.android.BreadcrumbType.MANUAL
import org.junit.Assert.assertEquals

import androidx.test.filters.SmallTest
import com.bugsnag.android.BugsnagTestUtils.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date

@SmallTest
class BreadcrumbStateSerializationTest {

    private lateinit var breadcrumbState: BreadcrumbState
    private var client: Client? = null

    @Before
    fun setUp() {
        breadcrumbState = BreadcrumbState(20, NoopLogger)
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
        val metadata = mutableMapOf<String, Any>(Pair("direction", "left"))
        breadcrumbState.add(Breadcrumb("Rotated Menu", BreadcrumbType.STATE, metadata, Date()))

        val json = streamableToJsonArray(breadcrumbState)
        val node = json.getJSONObject(0)
        assertEquals("Rotated Menu", node.get("name"))
        assertEquals("state", node.get("type"))
        assertEquals("left", node.getJSONObject("metaData").get("direction"))
        assertEquals(1, json.length())
    }

    /**
     * Verifies that the Client methods leave breadcrumbState correctly
     */
    @Test
    fun testClientMethods() {
        client!!.leaveBreadcrumb("Hello World")
        val store = client!!.breadcrumbState.store
        var count = 0

        for (breadcrumb in store) {
            if (MANUAL == breadcrumb.type && "manual" == breadcrumb.message
                && breadcrumb.metadata["message"] == "Hello World") {
                count++
            }
        }
        assertEquals(1, count)
    }

}
