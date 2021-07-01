package com.bugsnag.android

import com.bugsnag.android.internal.StateObserver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContextStateTest {

    @Test
    fun testDefaultContext() {
        assertNull(ContextState().getContext())
    }

    @Test
    fun testAutomaticContext() {
        val state = ContextState()

        // set automatic context
        state.setAutomaticContext("MyActivity")
        assertEquals("MyActivity", state.getContext())

        // override previous value
        state.setAutomaticContext("SecondActivity")
        assertEquals("SecondActivity", state.getContext())

        // set empty context
        state.setAutomaticContext("")
        assertEquals("", state.getContext())

        // set null context
        state.setAutomaticContext(null)
        assertNull(state.getContext())
    }

    @Test
    fun testManualContext() {
        val state = ContextState()

        // set manual context
        state.setManualContext("MyActivity")
        assertEquals("MyActivity", state.getContext())

        // override previous value
        state.setManualContext("SecondActivity")
        assertEquals("SecondActivity", state.getContext())

        // set empty context
        state.setManualContext("")
        assertEquals("", state.getContext())

        // set null context
        state.setManualContext(null)
        assertNull(state.getContext())
    }

    @Test
    fun testContextOverriding() {
        val state = ContextState()
        var event: StateEvent.UpdateContext? = null
        state.addObserver(StateObserver { event = it as StateEvent.UpdateContext })

        // set automatic context
        state.setAutomaticContext("MyActivity")
        assertEquals("MyActivity", state.getContext())
        assertEquals("MyActivity", requireNotNull(event).context)

        // setting a manual context overrides automatic context
        state.setManualContext("Foo")
        assertEquals("Foo", state.getContext())
        assertEquals("Foo", requireNotNull(event).context)

        // automatic context is always ignored after manual context is set
        state.setAutomaticContext("Bar")
        assertEquals("Foo", state.getContext())
        assertEquals("Foo", requireNotNull(event).context)

        state.setManualContext("")
        assertEquals("", state.getContext())
        assertEquals("", requireNotNull(event).context)

        state.setManualContext(null)
        assertNull(state.getContext())
        assertNull(requireNotNull(event).context)
    }
}
