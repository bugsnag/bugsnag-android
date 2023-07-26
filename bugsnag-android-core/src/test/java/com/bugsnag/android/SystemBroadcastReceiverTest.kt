package com.bugsnag.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.bugsnag.android.SystemBroadcastReceiver.Companion.shortenActionNameIfNeeded
import com.bugsnag.android.internal.ImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
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
        `when`(client.config).thenReturn(getConfig(emptySet()))
        `when`(intent.action).thenReturn("android.intent.action.AIRPLANE_MODE")

        val receiver = SystemBroadcastReceiver(client, NoopLogger)
        receiver.onReceive(context, intent)

        val metadata = mapOf(Pair("Intent Action", "android.intent.action.AIRPLANE_MODE"))
        verify(client, times(1)).leaveBreadcrumb("AIRPLANE_MODE", metadata, BreadcrumbType.STATE)
    }

    @Test
    fun testMetadataReceive() {
        `when`(client.config).thenReturn(getConfig(emptySet()))
        `when`(intent.action).thenReturn("SomeTitle")
        `when`(intent.extras).thenReturn(bundle)
        `when`(bundle.keySet()).thenReturn(setOf("foo"))
        @Suppress("DEPRECATION")
        `when`(bundle.get("foo")).thenReturn(setOf("bar"))

        val receiver = SystemBroadcastReceiver(client, NoopLogger)
        receiver.onReceive(context, intent)

        val metadata = mapOf(Pair("Intent Action", "SomeTitle"), Pair("foo", "[bar]"))
        verify(client, times(1)).leaveBreadcrumb("SomeTitle", metadata, BreadcrumbType.STATE)
    }

    @Test
    fun testBreadcrumbTypesUser() {
        `when`(client.config).thenReturn(getConfig(setOf(BreadcrumbType.USER)))
        val receiver = SystemBroadcastReceiver(client, NoopLogger)
        assertEquals(6, receiver.actions.size)
    }

    @Test
    fun testBreadcrumbTypesState() {
        `when`(client.config).thenReturn(getConfig(setOf(BreadcrumbType.STATE)))
        val receiver = SystemBroadcastReceiver(client, NoopLogger)
        assertEquals(25, receiver.actions.size)
    }

    @Test
    fun testBreadcrumbTypesNavigation() {
        `when`(client.config).thenReturn(getConfig(setOf(BreadcrumbType.NAVIGATION)))
        val receiver = SystemBroadcastReceiver(client, NoopLogger)
        assertEquals(2, receiver.actions.size)
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

    private fun getConfig(breadcrumbTypes: Set<BreadcrumbType>): ImmutableConfig {
        val config = BugsnagTestUtils.generateConfiguration()
        config.enabledBreadcrumbTypes = breadcrumbTypes
        return BugsnagTestUtils.convert(config)
    }
}
