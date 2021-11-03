package com.bugsnag.android.internal.journal

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Serializing mixed type hierarchies should not throw an exception
 */
class JsonHelperTest {

    @Test
    fun testSerializingMixedTypes() {
        val baos = ByteArrayOutputStream()
        val mixedTypes: List<Any> = listOf(
            listOf(1, 2),
            listOf(true, false),
            listOf("a", "z")
        )

        val map: Map<String, Any> = mapOf("list" to mixedTypes)
        JsonHelper.serialize(map, baos)
        assertEquals("{\"list\":\"[OBJECT]\"}", String(baos.toByteArray()))
    }

    @Test
    fun testSerializingMixedTypesArray() {
        val baos = ByteArrayOutputStream()
        val mixedTypes: Array<Any> = arrayOf(
            arrayOf(1, 2),
            arrayOf(true, false),
            arrayOf("a", "z")
        )

        val map: Map<String, Any> = mapOf("array" to mixedTypes)
        JsonHelper.serialize(map, baos)
        assertEquals("{\"array\":\"[OBJECT]\"}", String(baos.toByteArray()))
    }

    @Test
    fun testSerializingAnyCollection() {
        val baos = ByteArrayOutputStream()
        val mixedTypes: List<Any> = listOf(
            true,
            arrayOf(1, 2)
        )

        val map: Map<String, Any> = mapOf("collection" to mixedTypes)
        JsonHelper.serialize(map, baos)
        assertEquals("{\"collection\":[true,\"[OBJECT]\"}", String(baos.toByteArray()))
    }
}
