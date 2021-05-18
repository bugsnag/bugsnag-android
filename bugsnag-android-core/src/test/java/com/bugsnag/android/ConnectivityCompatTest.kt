package com.bugsnag.android

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ConnectivityCompatTest {

    @Mock
    lateinit var connectivityManager: ConnectivityManager

    @Mock
    lateinit var context: Context

    @Test
    fun testWithConnectivityManager() {
        Mockito.`when`(context.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(connectivityManager)

        val connectivity = ConnectivityCompat(context, null)
        assertFalse(connectivity.hasNetworkConnection())
    }

    /**
     * If getSystemService throws a SecurityException, ConnectivityCompat should still init without
     * an exception
     */
    @Test
    fun initWithSecurityException() {
        Mockito.`when`(context.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenThrow(SecurityException())

        ConnectivityCompat(context, null)
    }

    /**
     * Test that ConnectivityCompat still works when there is no ConnectivityManager available
     */
    @Test
    fun testNoConnectivityManager() {
        val connectivity = ConnectivityCompat(context, null)
        assertTrue(connectivity.hasNetworkConnection())
        assertEquals("unknown", connectivity.retrieveNetworkAccessState())
    }

    @Test
    fun handlesConnectivityManagerExceptions() {
        Mockito.`when`(context.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(connectivityManager)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Mockito.`when`(connectivityManager.activeNetwork)
                .thenThrow(SecurityException())
        } else {
            @Suppress("DEPRECATION")
            Mockito.`when`(connectivityManager.activeNetworkInfo)
                .thenThrow(SecurityException())
        }

        val connectivity = ConnectivityCompat(context, null)
        assertTrue(connectivity.hasNetworkConnection())
        assertEquals("unknown", connectivity.retrieveNetworkAccessState())
    }
}
