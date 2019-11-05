package com.bugsnag.android

import android.content.Context
import android.net.ConnectivityManager
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner

@Suppress("DEPRECATION")
@RunWith(MockitoJUnitRunner::class)
class ConnectivityLegacyTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var cm: ConnectivityManager

    @Test
    fun registerForNetworkChanges() {
        val conn = ConnectivityLegacy(context, cm, null)
        conn.registerForNetworkChanges()
        Mockito.verify(context, times(1)).registerReceiver(any(), any())
    }

    @Test
    fun unregisterForNetworkChanges() {
        val conn = ConnectivityLegacy(context, cm, null)
        conn.unregisterForNetworkChanges()
        Mockito.verify(context, times(1)).unregisterReceiver(any())
    }

    @Test
    fun hasNetworkConnection() {
        val conn = ConnectivityLegacy(context, cm, null)
        assertFalse(conn.hasNetworkConnection())
    }

    @Test
    fun networkAccessState() {
        val conn = ConnectivityLegacy(context, cm, null)
        assertEquals("none", conn.retrieveNetworkAccessState())
    }
}
