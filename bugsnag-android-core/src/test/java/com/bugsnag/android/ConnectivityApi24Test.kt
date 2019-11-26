package com.bugsnag.android

import android.net.ConnectivityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ConnectivityApi24Test {

    @Mock
    lateinit var cm: ConnectivityManager

    @Test
    fun registerForNetworkChanges() {
        val conn = ConnectivityApi24(cm) { }
        conn.registerForNetworkChanges()
        verify(cm, times(1)).registerDefaultNetworkCallback(any())
    }

    @Test
    fun unregisterForNetworkChanges() {
        val conn = ConnectivityApi24(cm) { }
        conn.unregisterForNetworkChanges()
        verify(cm, times(1)).unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>())
    }

    @Test
    fun hasNetworkConnection() {
        val conn = ConnectivityApi24(cm) { }
        assertFalse(conn.hasNetworkConnection())
    }

    @Test
    fun networkAccessState() {
        val conn = ConnectivityApi24(cm) {}
        assertEquals("none", conn.retrieveNetworkAccessState())
    }
}
