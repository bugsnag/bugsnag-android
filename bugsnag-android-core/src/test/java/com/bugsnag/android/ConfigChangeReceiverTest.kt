package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ConfigChangeReceiverTest {

    private lateinit var receiver: ConfigChangeReceiver

    @Mock
    private lateinit var deviceDataCollector: DeviceDataCollector

    @Test
    fun verifyDefaultOrientation() {
        `when`(deviceDataCollector.calculateOrientation()).thenReturn("portrait")
        receiver = ConfigChangeReceiver(deviceDataCollector) { _, _ -> }
        assertEquals("portrait", receiver.orientation)
    }

    @Test
    fun verifyOrientationChange() {
        `when`(deviceDataCollector.calculateOrientation()).thenReturn("portrait")
        var old: String? = null
        var new: String? = null

        receiver = ConfigChangeReceiver(deviceDataCollector) { a, b ->
            old = a
            new = b
        }

        `when`(deviceDataCollector.calculateOrientation()).thenReturn("landscape")
        receiver.onReceive(null, null)
        assertEquals("portrait", old)
        assertEquals("landscape", new)
    }

    @Test
    fun unrelatedConfigurationChange() {
        `when`(deviceDataCollector.calculateOrientation()).thenReturn("portrait")
        var old: String? = null
        var new: String? = null

        receiver = ConfigChangeReceiver(deviceDataCollector) { a, b ->
            old = a
            new = b
        }

        // orientation has not changed, don't invoke a callback
        `when`(deviceDataCollector.calculateOrientation()).thenReturn("portrait")
        receiver.onReceive(null, null)
        assertNull(old)
        assertNull(new)
    }
}
