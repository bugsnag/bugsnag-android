package com.bugsnag.android

import android.content.ComponentCallbacks2
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class MemoryTrimStateTest {

    lateinit var memoryTrimState: MemoryTrimState

    @Before
    fun setUp() {
        memoryTrimState = MemoryTrimState()
    }

    @Test
    fun memoryTrimLevelDescriptions() {
        memoryTrimState.memoryTrimLevel = ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE
        assertEquals("Running moderate", memoryTrimState.trimLevelDescription)

        memoryTrimState.memoryTrimLevel = ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
        assertEquals("Running low", memoryTrimState.trimLevelDescription)

        memoryTrimState.memoryTrimLevel = ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        assertEquals("Running critical", memoryTrimState.trimLevelDescription)

        memoryTrimState.memoryTrimLevel = ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        assertEquals("UI hidden", memoryTrimState.trimLevelDescription)

        memoryTrimState.memoryTrimLevel = ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
        assertEquals("Background", memoryTrimState.trimLevelDescription)

        memoryTrimState.memoryTrimLevel = ComponentCallbacks2.TRIM_MEMORY_MODERATE
        assertEquals("Moderate", memoryTrimState.trimLevelDescription)

        memoryTrimState.memoryTrimLevel = ComponentCallbacks2.TRIM_MEMORY_COMPLETE
        assertEquals("Complete", memoryTrimState.trimLevelDescription)

        memoryTrimState.memoryTrimLevel = null
        assertEquals("None", memoryTrimState.trimLevelDescription)
    }

    @Test
    fun unknownMemoryTrimLevel() {
        memoryTrimState.memoryTrimLevel = ComponentCallbacks2.TRIM_MEMORY_COMPLETE + 1
        assertEquals(
            "Unknown (${memoryTrimState.memoryTrimLevel})",
            memoryTrimState.trimLevelDescription
        )
    }
}
