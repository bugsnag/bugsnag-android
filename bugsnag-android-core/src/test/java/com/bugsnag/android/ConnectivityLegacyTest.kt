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
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ConnectivityLegacyTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var cm: ConnectivityManager

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
