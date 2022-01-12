package com.bugsnag.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ContextExtensionsKtTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var receiver: BroadcastReceiver

    @Mock
    lateinit var filter: IntentFilter

    @Test
    fun registerReceiverSafe() {
        context.registerReceiverSafe(receiver, filter)
        verify(context, times(1)).registerReceiver(receiver, filter)
    }

    @Test
    fun registerReceiverSecurityException() {
        val logger = InterceptingLogger()
        `when`(context.registerReceiver(receiver, filter)).thenThrow(SecurityException())
        context.registerReceiverSafe(receiver, filter, logger)
        assertEquals("Failed to register receiver", logger.msg)
    }

    @Test
    fun unregisterReceiverSafe() {
        context.unregisterReceiverSafe(receiver)
        verify(context, times(1)).unregisterReceiver(receiver)
    }

    @Test
    fun unregisterReceiverSecurityException() {
        val logger = InterceptingLogger()
        `when`(context.unregisterReceiver(receiver)).thenThrow(SecurityException())
        context.unregisterReceiverSafe(receiver, logger)
        assertEquals("Failed to register receiver", logger.msg)
    }
}
