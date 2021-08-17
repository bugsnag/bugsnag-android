package com.bugsnag.android

import android.content.ComponentCallbacks2
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ClientComponentCallbacksMemoryCallbackTest {

    @Mock
    lateinit var context: Context

    @Mock
    internal lateinit var deviceDataCollector: DeviceDataCollector
    private lateinit var clientComponentCallbacks: ClientComponentCallbacks

    private var isLowMemory: Boolean? = null

    @Before
    fun setUp() {
        clientComponentCallbacks = ClientComponentCallbacks(
            deviceDataCollector,
            { _: String?, _: String? -> }
        ) { lowMemory, _ -> isLowMemory = lowMemory }
        isLowMemory = null
    }

    @Test
    fun testLegacyLowMemory() {
        clientComponentCallbacks.onLowMemory()
        assertEquals(true, isLowMemory)
    }

    @Test
    fun trimMemoryRunningModerate() {
        clientComponentCallbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE)
        assertEquals(false, isLowMemory)
    }

    @Test
    fun trimMemoryRunningLow() {
        clientComponentCallbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
        assertEquals(false, isLowMemory)
    }

    @Test
    fun trimMemoryRunningCritical() {
        clientComponentCallbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
        assertEquals(false, isLowMemory)
    }

    @Test
    fun trimMemoryUiHidden() {
        clientComponentCallbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        assertEquals(false, isLowMemory)
    }

    @Test
    fun trimMemoryBackground() {
        clientComponentCallbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
        assertEquals(false, isLowMemory)
    }

    @Test
    fun trimMemoryModerate() {
        clientComponentCallbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_MODERATE)
        assertEquals(false, isLowMemory)
    }

    @Test
    fun trimMemoryComplete() {
        clientComponentCallbacks.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        assertEquals(true, isLowMemory)
    }
}
