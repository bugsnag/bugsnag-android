@file:Suppress("DEPRECATION")

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
    fun connectivityLegacyHasConnection() {
        val conn = ConnectivityApi24(cm, null)
        assertFalse(conn.hasNetworkConnection())
        Mockito.`when`(cm.activeNetwork).thenReturn(info)
        assertTrue(conn.hasNetworkConnection())
    }

    @Test
    fun connectivityLegacyNetworkState() {
        val conn = ConnectivityApi24(cm, null)
        Mockito.`when`(cm.activeNetwork).thenReturn(info)

        Mockito.`when`(cm.getNetworkCapabilities(info)).thenReturn(null)
        assertEquals("none", conn.retrieveNetworkAccessState())

        Mockito.`when`(cm.getNetworkCapabilities(info)).thenReturn(capabilities)
        Mockito.`when`(capabilities.hasTransport(0)).thenReturn(true)
        assertEquals("cellular", conn.retrieveNetworkAccessState())
    }
}
