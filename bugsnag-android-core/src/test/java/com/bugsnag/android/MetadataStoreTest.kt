package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

internal class MetadataStoreTest {

    lateinit var metadata: Metadata

    @Before
    fun setUp() {
        metadata = Metadata()
    }

    @Test
    fun addNewValueToToplevel() {
        metadata.addMetadata("a", "foo")
        assertEquals(mapOf(Pair("a", "foo")), metadata.toMap())
    }

    @Test
    fun addNewValueToTab() {
        metadata.addMetadata("a", "b", "foo")
        assertEquals(mapOf(Pair("a", mapOf(Pair("b", "foo")))), metadata.toMap())
    }

    @Test
    fun addNewValueToExistingTab() {
        metadata.addMetadata("a", "b", "foo")
        metadata.addMetadata("a", "c", true)
        assertEquals(mapOf(Pair("a", mapOf(Pair("b", "foo"), Pair("c", true)))), metadata.toMap())
    }

    @Test
    fun addNullValueToExistingTab() {
        metadata.addMetadata("a", "b", "foo")
        metadata.addMetadata("a", "c", null)
        assertEquals(mapOf(Pair("a", mapOf(Pair("b", "foo")))), metadata.toMap())
    }

    @Test
    fun overrideExistingTopLevel() {
        metadata.addMetadata("a", "foo")
        metadata.addMetadata("a", "bar")
        assertEquals(mapOf(Pair("a", "bar")), metadata.toMap())
    }

    @Test
    fun overrideExistingTopLevelDiffType() {
        metadata.addMetadata("a", "foo")
        metadata.addMetadata("a", 55.6)
        assertEquals(mapOf(Pair("a", 55.6)), metadata.toMap())
    }

    @Test
    fun overrideExistingTopLevelNull() {
        metadata.addMetadata("a", "foo")
        metadata.addMetadata("a", null)
        assertEquals(emptyMap<String, Any?>(), metadata.toMap())
    }

    @Test
    fun overrideExistingTab() {
        metadata.addMetadata("a", "b", "foo")
        metadata.addMetadata("a", "b", "bar")
        assertEquals(mapOf(Pair("a", mapOf(Pair("b", "bar")))), metadata.toMap())
    }

    @Test
    fun overrideExistingTabDiffType() {
        metadata.addMetadata("a", "b", "foo")
        metadata.addMetadata("a", "b", 500L)
        assertEquals(mapOf(Pair("a", mapOf(Pair("b", 500L)))), metadata.toMap())
    }

    @Test
    fun overrideExistingTabNull() {
        metadata.addMetadata("a", "b", "foo")
        metadata.addMetadata("a", "b", null)
        assertEquals(emptyMap<String, Any?>(), metadata.toMap())
    }

    @Test
    fun getExistingTopLevel() {
        metadata.addMetadata("a", "foo")
        assertEquals("foo", metadata.getMetadata("a"))
    }

    @Test
    fun getNonExistingTopLevel() {
        assertNull(metadata.getMetadata("foo"))
    }

    @Test
    fun getExistingTab() {
        metadata.addMetadata("a", "b", "foo")
        assertEquals(mapOf<String, Any?>(Pair("b", "foo")), metadata.getMetadata("a"))
        assertEquals("foo", metadata.getMetadata("a", "b"))
    }

    @Test
    fun getNonExistingTab() {
        assertNull(metadata.getMetadata("foo", "bar"))
    }

    @Test
    fun clearTopLevel() {
        metadata.addMetadata("a", "foo")
        metadata.clearMetadata("a")
        assertEquals(emptyMap<String, Any?>(), metadata.toMap())
    }

    @Test
    fun clearTabOtherValues() {
        metadata.addMetadata("a", "b", "foo")
        metadata.addMetadata("a", "c", "bar")
        metadata.clearMetadata("a", "b")
        assertEquals(mapOf(Pair("a", mapOf(Pair("c", "bar")))), metadata.toMap())
    }

    @Test
    fun clearTabNoOtherValues() {
        metadata.addMetadata("a", "b", "foo")
        metadata.clearMetadata("a", "b")
        assertEquals(emptyMap<String, Any?>(), metadata.toMap())
    }

    @Test
    fun addMapTopLevel() {
        metadata.addMetadata("a", mapOf(Pair("b", "foo")))
        assertEquals(mapOf(Pair("a", mapOf(Pair("b", "foo")))), metadata.toMap())
    }

    @Test
    fun addMapTopLevelExistingObj() {
        metadata.addMetadata("a", 5)
        metadata.addMetadata("a", mapOf(Pair("b", "foo")))
        assertEquals(mapOf(Pair("a", mapOf(Pair("b", "foo")))), metadata.toMap())
    }

    @Test
    fun addMapTopLevelExistingMap() {
        metadata.addMetadata("a", mapOf(Pair("b", "foo")))
        metadata.addMetadata("a", mapOf(Pair("c", "bar")))
        assertEquals(mapOf(Pair("a", mapOf(Pair("b", "foo"), Pair("c", "bar")))), metadata.toMap())
    }

    @Test
    fun addMapTab() {
        metadata.addMetadata("a", "b", mapOf(Pair("c", "foo")))
        assertEquals(mapOf(Pair("a", mapOf(Pair("b", mapOf(Pair("c", "foo")))))), metadata.toMap())
    }

    @Test
    fun addMapTabExistingObj() {
        metadata.addMetadata("a", "b", 22)
        metadata.addMetadata("a", "b", mapOf(Pair("c", "foo")))
        assertEquals(mapOf(Pair("a", mapOf(Pair("b", mapOf(Pair("c", "foo")))))), metadata.toMap())
    }

    @Test
    fun addMapTabExistingMap() {
        metadata.addMetadata("a", "b", mapOf(Pair("c", "foo")))
        metadata.addMetadata("a", "b", mapOf(Pair("d", "bar")))
        assertEquals(mapOf(Pair("a", mapOf(Pair("b", mapOf(Pair("c", "foo"), Pair("d", "bar")))))), metadata.toMap())
    }

    @Test
    fun addMetadataMergeBasicMap() {
        metadata.addMetadata("a", "b", mapOf(Pair("c", "foo")))
        metadata.addMetadata("a", "b", mapOf(Pair("c", "bar")))
        assertEquals(mapOf(Pair("a", mapOf(Pair("b", mapOf(Pair("c", "bar")))))), metadata.toMap())
    }

    @Test
    fun addMetadataMergeNullKeys() {
        metadata.addMetadata("a", "b", mapOf(Pair("c", "foo")))
        metadata.addMetadata("a", "b", mapOf(Pair("c", null)))
        assertEquals(mapOf(Pair("a", mapOf(Pair("b", mapOf(Pair("c", "foo")))))), metadata.toMap())
    }

    @Test
    fun addMetadataMergeNestedMap() {
        metadata.addMetadata("a", "b", mapOf(Pair("c", mapOf(Pair("d", 5)))))
        metadata.addMetadata("a", "b", mapOf(Pair("c", mapOf(Pair("d", true)))))
        assertEquals(mapOf(Pair("a", mapOf(Pair("b", mapOf(Pair("c", mapOf(Pair("d", true)))))))), metadata.toMap())
    }
}
