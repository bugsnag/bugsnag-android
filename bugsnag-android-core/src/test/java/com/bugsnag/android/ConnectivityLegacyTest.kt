@file:Suppress("DEPRECATION")

package com.bugsnag.android

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner

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

    @Mock
    lateinit var info: NetworkInfo

    @Test
    fun connectivityLegacyHasConnection() {
        val conn = ConnectivityLegacy(context, cm, null)
        assertFalse(conn.hasNetworkConnection())

        Mockito.`when`(cm.activeNetworkInfo).thenReturn(info)
        Mockito.`when`(info.isConnectedOrConnecting).thenReturn(true)
        assertTrue(conn.hasNetworkConnection())
    }

    @Test
    fun connectivityLegacyNetworkState() {
        val conn = ConnectivityLegacy(context, cm, null)

        Mockito.`when`(cm.activeNetworkInfo).thenReturn(info)
        Mockito.`when`(info.type).thenReturn(99)
        assertEquals("cellular", conn.retrieveNetworkAccessState())

        Mockito.`when`(info.type).thenReturn(1)
        assertEquals("wifi", conn.retrieveNetworkAccessState())
    }
}
