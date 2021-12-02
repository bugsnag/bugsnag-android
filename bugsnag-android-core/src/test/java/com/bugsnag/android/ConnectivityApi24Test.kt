package com.bugsnag.android

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ConnectivityApi24Test {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var cm: ConnectivityManager

    @Mock
    lateinit var info: Network

    @Mock
    lateinit var capabilities: NetworkCapabilities

    @Test
    fun connectivityApi24HasConnection() {
        val conn = ConnectivityApi24(cm, null)
        assertFalse(conn.hasNetworkConnection())
        Mockito.`when`(cm.activeNetwork).thenReturn(info)
        assertTrue(conn.hasNetworkConnection())
    }

    @Test
    fun connectivityApi24NetworkState() {
        val conn = ConnectivityApi24(cm, null)
        Mockito.`when`(cm.activeNetwork).thenReturn(info)

        Mockito.`when`(cm.getNetworkCapabilities(info)).thenReturn(null)
        assertEquals("none", conn.retrieveNetworkAccessState())

        Mockito.`when`(cm.getNetworkCapabilities(info)).thenReturn(capabilities)
        Mockito.`when`(capabilities.hasTransport(0)).thenReturn(true)
        assertEquals("cellular", conn.retrieveNetworkAccessState())
    }

    @Test
    fun connectivityApi24OnAvailable() {
        var count = 0
        val tracker = ConnectivityApi24.ConnectivityTrackerCallback { _, _ ->
            count++
        }
        assertEquals(0, count)

        tracker.onAvailable(info)
        assertEquals(0, count)

        tracker.onAvailable(info)
        assertEquals(1, count)

        tracker.onAvailable(info)
        assertEquals(2, count)
    }

    @Test
    fun connectivityApi24OnUnvailable() {
        var count = 0
        val tracker = ConnectivityApi24.ConnectivityTrackerCallback { _, _ ->
            count++
        }
        assertEquals(0, count)

        tracker.onUnavailable()
        assertEquals(0, count)

        tracker.onUnavailable()
        assertEquals(1, count)

        tracker.onUnavailable()
        assertEquals(2, count)
    }
}
