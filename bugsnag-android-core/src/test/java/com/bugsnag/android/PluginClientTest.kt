package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PluginClientTest {

    @Mock
    lateinit var plugin: Plugin

    @Mock
    lateinit var client: Client

    internal val config = BugsnagTestUtils.generateImmutableConfig()

    @Test
    fun loadCustomPlugin() {
        val pluginClient = PluginClient(setOf(plugin), config, NoopLogger)
        pluginClient.loadPlugins(client)
        Mockito.verify(plugin, times(1)).load(client)
    }

    @Test
    fun findPlugin() {
        val pluginClient = PluginClient(setOf(plugin), config, NoopLogger)
        assertNull(pluginClient.findPlugin(String::class.java))
        assertEquals(plugin, pluginClient.findPlugin(plugin::class.java))
    }
}
