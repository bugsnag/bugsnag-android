package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class MetadataStateTest {

    lateinit var state: MetadataState

    @Before
    fun setUp() {
        state = MetadataState()
    }

    @Test
    fun addTopLevel() {
        state.addMetadata("foo", mapOf(Pair("wham", "bar")))
        val tab = state.getMetadata("foo")
        assertEquals(mapOf(Pair("wham", "bar")), tab)
    }

    @Test
    fun addWithKey() {
        state.addMetadata("foo", "wham", "baz")
        val topLevel = state.getMetadata("foo") as Map<*, *>
        val tab = state.getMetadata("foo", "wham")
        assertEquals("baz", topLevel["wham"])
        assertEquals("baz", tab)
    }

    @Test
    fun clearTopLevel() {
        state.addMetadata("foo", mapOf(Pair("wham", "bar")))
        state.clearMetadata("foo")
        assertNull(state.getMetadata("foo"))
    }

    @Test
    fun clearWithKey() {
        state.addMetadata("foo", "wham", "baz")
        state.clearMetadata("foo", "wham")
        assertNull(state.getMetadata("foo"))
    }
}
