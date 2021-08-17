package com.bugsnag.android

import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class UpdateMemoryTrimLevelTest {

    lateinit var memoryTrimState: MemoryTrimState

    @Before
    fun setUp() {
        memoryTrimState = MemoryTrimState()
    }

    @Test
    fun updateFromNull() {
        assertNull(memoryTrimState.memoryTrimLevel)
        assertTrue(memoryTrimState.updateMemoryTrimLevel(TRIM_MEMORY_COMPLETE))
        assertEquals(TRIM_MEMORY_COMPLETE, memoryTrimState.memoryTrimLevel)
    }

    @Test
    fun updateWithoutChange() {
        assertTrue(memoryTrimState.updateMemoryTrimLevel(TRIM_MEMORY_COMPLETE))
        assertFalse(memoryTrimState.updateMemoryTrimLevel(TRIM_MEMORY_COMPLETE))
    }

    @Test
    fun multipleChanges() {
        assertTrue(memoryTrimState.updateMemoryTrimLevel(TRIM_MEMORY_COMPLETE))
        assertTrue(memoryTrimState.updateMemoryTrimLevel(TRIM_MEMORY_RUNNING_LOW))
        assertTrue(memoryTrimState.updateMemoryTrimLevel(TRIM_MEMORY_RUNNING_MODERATE))
        assertEquals(TRIM_MEMORY_RUNNING_MODERATE, memoryTrimState.memoryTrimLevel)
    }

    @Test
    fun updateToNull() {
        assertTrue(memoryTrimState.updateMemoryTrimLevel(TRIM_MEMORY_COMPLETE))
        assertTrue(memoryTrimState.updateMemoryTrimLevel(null))
        assertNull(memoryTrimState.memoryTrimLevel)
    }
}
