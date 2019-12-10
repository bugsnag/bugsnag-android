package com.bugsnag.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.bugsnag.android.SystemBroadcastReceiver.shortenActionNameIfNeeded
import org.junit.Assert.assertEquals

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SystemBroadcastReceiverTest {

    @Mock
    lateinit var client: Client

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var intent: Intent

    @Mock
    lateinit var bundle: Bundle

    @Test
    fun testBasicReceive() {
        `when`(intent.action).thenReturn("android.intent.action.AIRPLANE_MODE")


        val receiver = SystemBroadcastReceiver(client, NoopLogger)
        receiver.onReceive(context, intent)

        val metadata = mapOf(Pair("Intent Action", "android.intent.action.AIRPLANE_MODE"))
        verify(client, times(1)).leaveBreadcrumb("AIRPLANE_MODE", BreadcrumbType.STATE, metadata)
    }

    @Test
    fun testMetadataReceive() {
        `when`(intent.action).thenReturn("SomeTitle")
        `when`(intent.extras).thenReturn(bundle)
        `when`(bundle.keySet()).thenReturn(setOf("foo"))
        `when`(bundle.get("foo")).thenReturn(setOf("bar"))

        val receiver = SystemBroadcastReceiver(client, NoopLogger)
        receiver.onReceive(context, intent)

        val metadata = mapOf(Pair("Intent Action", "SomeTitle"), Pair("foo", "[bar]"))
        verify(client, times(1)).leaveBreadcrumb("SomeTitle", BreadcrumbType.LOG, metadata)
    }

    @Test
    fun checkActionName() {
        assertEquals(
            "CONNECTION_STATE_CHANGE",
            shortenActionNameIfNeeded("android.net.wifi.p2p.CONNECTION_STATE_CHANGE")
        )

        assertEquals(
            "CONNECTION_STATE_CHANGE",
            shortenActionNameIfNeeded("CONNECTION_STATE_CHANGE")
        )

        assertEquals(
            "NOT_ANDROID.net.wifi.p2p.CONNECTION_STATE_CHANGE",
            shortenActionNameIfNeeded("NOT_ANDROID.net.wifi.p2p.CONNECTION_STATE_CHANGE")
        )
    }

}
