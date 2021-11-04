package com.bugsnag.android

import com.bugsnag.android.internal.StateObserver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun initObservableMessages() {
        state.addMetadata("foo", "wham", "baz")
        state.addMetadata("bar", "another", true)

        val data = mutableSetOf<String>()
        state.addObserver(
            StateObserver {
                val msg = it as StateEvent.AddMetadata
                data.add(msg.section)
            }
        )

        state.emitObservableEvent()
        assertEquals(setOf("foo", "bar"), data)
    }

    @Test
    fun initObservableMessagesWithMap() {
        state.addMetadata(
            "foo",
            hashMapOf(
                "key1" to "1",
                "key2" to "2",
                "key3" to 3
            )
        )

        val sections = mutableSetOf<String>()
        val keys = mutableSetOf<String?>()
        state.addObserver(
            StateObserver {
                val msg = it as StateEvent.AddMetadata
                sections.add(msg.section)
                keys.add(msg.key)
            }
        )

        state.emitObservableEvent()
        assertEquals(setOf("foo"), sections)
        assertEquals(setOf("key1", "key2", "key3"), keys)
    }

    @Test
    fun observableMessageWithMap() {
        val events = mutableListOf<StateEvent.AddMetadata>()
        state.addObserver(
            StateObserver {
                events.add(it as StateEvent.AddMetadata)
            }
        )

        state.addMetadata("fruit", mapOf<String, Any?>("apple" to "gala"))
        assertEquals("fruit", events[0].section)
        assertEquals("apple", events[0].key)
        assertEquals("gala", events[0].value)
    }
}
