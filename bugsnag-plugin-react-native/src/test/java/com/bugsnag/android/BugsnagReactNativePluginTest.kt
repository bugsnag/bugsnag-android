package com.bugsnag.android

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class BugsnagReactNativePluginTest {

    @Mock
    lateinit var client: Client

    @Mock
    lateinit var internalHooks: InternalHooks

    private lateinit var plugin: BugsnagReactNativePlugin

    @Before
    fun setUp() {
        plugin = BugsnagReactNativePlugin()
        plugin.client = client
        plugin.internalHooks = internalHooks
        plugin.logger = object : Logger {}
    }

    @Test
    fun registerForMessageEvents() {
        plugin.registerForMessageEvents { }
        verify(client, times(1)).syncInitialState()
    }

    @Test
    fun startSession() {
        plugin.startSession()
        verify(client, times(1)).startSession()
    }

    @Test
    fun pauseSession() {
        plugin.pauseSession()
        verify(client, times(1)).pauseSession()
    }

    @Test
    fun resumeSession() {
        plugin.resumeSession()
        verify(client, times(1)).resumeSession()
    }

    @Test
    fun updateContext() {
        plugin.updateContext("Foo")
        verify(client, times(1)).context = "Foo"
    }

    @Test
    fun updateUser() {
        plugin.updateUser("123", "joe@example.com", "Joe")
        verify(client, times(1)).setUser("123", "joe@example.com", "Joe")
    }

    @Test
    fun updateCodeBundleId() {
        plugin.updateCodeBundleId("123")
        verify(client, times(1)).codeBundleId = "123"
    }

    @Test
    fun addFeatureFlag() {
        plugin.addFeatureFlag("flag name", "variant")
        verify(client, times(1)).addFeatureFlag("flag name", "variant")
    }

    @Test
    fun clearFeatureFlag() {
        plugin.clearFeatureFlag("flag name")
        verify(client, times(1)).clearFeatureFlag("flag name")
    }

    @Test
    fun clearFeatureFla() {
        plugin.clearFeatureFlags()
        verify(client, times(1)).clearFeatureFlags()
    }
}
