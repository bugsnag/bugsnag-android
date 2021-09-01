package com.bugsnag.android

import com.bugsnag.android.BreadcrumbType.NAVIGATION
import com.bugsnag.android.internal.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Collections
import java.util.Date
import java.util.HashMap

class BreadcrumbDeserializerTest {

    private val map = HashMap<String, Any>()
    private val metadata = HashMap<String, Any>()

    /**
     * Generates a map for verifying the serializer
     */
    @Before
    fun setup() {
        metadata["custom"] = hashMapOf(
            "id" to 123,
            "surname" to "Bloggs"
        )
        metadata["data"] = Collections.singletonMap("optIn", true)
        map["metadata"] = metadata
        map["message"] = "Whoops"
        map["type"] = "navigation"
        map["timestamp"] = DateUtils.toIso8601(Date(0))
    }

    @Test
    fun deserialize() {
        val crumb = BreadcrumbDeserializer(object : Logger {}).deserialize(map)
        assertEquals("Whoops", crumb.message)
        assertEquals(NAVIGATION, crumb.type)
        assertEquals(Date(0).time, crumb.timestamp.time)
        assertEquals(metadata, crumb.metadata)
    }
}
