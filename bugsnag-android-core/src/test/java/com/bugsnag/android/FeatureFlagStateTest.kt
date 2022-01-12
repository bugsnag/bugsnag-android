package com.bugsnag.android

import com.bugsnag.android.internal.StateObserver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class FeatureFlagStateTest {
    lateinit var events: MutableList<StateEvent>
    lateinit var state: FeatureFlagState

    @Before
    fun setUp() {
        events = mutableListOf()
        state = FeatureFlagState()
        state.addObserver(StateObserver { events.add(it) })
    }

    @Test
    fun addFeatureFlag_NoVariant() {
        state.addFeatureFlag("demo_mode")

        val featureFlag = state.toList().single()
        assertEquals(FeatureFlag("demo_mode"), featureFlag)

        val event = events.single() as StateEvent.AddFeatureFlag
        assertEquals("demo_mode", event.name)
        assertNull(event.variant)
    }

    @Test
    fun addFeatureFlag() {
        state.addFeatureFlag("sample_group", "a")

        val featureFlag = state.toList().single()
        assertEquals(FeatureFlag("sample_group", "a"), featureFlag)

        val event = events.single() as StateEvent.AddFeatureFlag
        assertEquals("sample_group", event.name)
        assertEquals("a", event.variant)
    }

    @Test
    fun addFeatureFlags() {
        val flags = listOf(
            FeatureFlag("demo_mode"),
            FeatureFlag("sample_group", "1234")
        )

        state.addFeatureFlags(flags)

        val featureFlags = state.toList()
        assertEquals(2, featureFlags.size)
        assertTrue(featureFlags.containsAll(flags))

        assertEquals(2, events.size)
        assertTrue(events[0] is StateEvent.AddFeatureFlag)
        assertTrue(events[1] is StateEvent.AddFeatureFlag)
    }

    @Test
    fun clearFeatureFlag() {
        state.addFeatureFlag("sample_group", "4321")
        state.clearFeatureFlag("sample_group")

        val featureFlags = state.toList()
        assertEquals(0, featureFlags.size)

        assertEquals(2, events.size)
        val addEvent = events.find { it is StateEvent.AddFeatureFlag } as StateEvent.AddFeatureFlag
        assertEquals("sample_group", addEvent.name)
        assertEquals("4321", addEvent.variant)

        assertNotNull(events.find { it is StateEvent.ClearFeatureFlag })
    }

    @Test
    fun clearFeatureFlags() {
        state.addFeatureFlag("sample_group", "4321")
        state.addFeatureFlag("listing_view", "legacy")
        state.addFeatureFlag("demo_mode")
        state.clearFeatureFlags()

        val featureFlags = state.toList()
        assertEquals(0, featureFlags.size)

        assertEquals(4, events.size)
        val addEvents = events.filterIsInstance<StateEvent.AddFeatureFlag>()
        assertEquals(3, addEvents.size)

        assertNotNull(events.find { it is StateEvent.ClearFeatureFlags })
    }

    @Test
    fun emitObservableEvent() {
        state.addFeatureFlag("sample_group", "4321")
        state.addFeatureFlag("listing_view", "legacy")
        state.addFeatureFlag("demo_mode")

        // clear the events
        events.clear()

        state.emitObservableEvent()
        assertEquals(3, events.size)
        val addSampleGroup = events
            .filterIsInstance<StateEvent.AddFeatureFlag>()
            .find { it.name == "sample_group" }
        assertEquals("4321", addSampleGroup?.variant)

        val addListingView = events
            .filterIsInstance<StateEvent.AddFeatureFlag>()
            .find { it.name == "listing_view" }
        assertEquals("legacy", addListingView?.variant)

        val addDemoMode = events
            .filterIsInstance<StateEvent.AddFeatureFlag>()
            .find { it.name == "demo_mode" }
        assertNotNull(addDemoMode)
        assertNull(addDemoMode!!.variant)
    }
}
