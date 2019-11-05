package com.bugsnag.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BugsnagPluginInterfaceTest {

    @Mock
    lateinit var client: Client

    @Test
    fun loadUnloadPlugin() {
        BugsnagPluginInterface.loadPlugin(client, FakePlugin::class.java)
        assertTrue(FakePlugin.active)
        BugsnagPluginInterface.unloadPlugin(FakePlugin::class.java)
        assertFalse(FakePlugin.active)
    }

    @Test
    fun noExceptionThrownWithInvalidPlugin() {
        BugsnagPluginInterface.loadPlugin(client, String::class.java)
        BugsnagPluginInterface.unloadPlugin(String::class.java)
    }
}

internal class FakePlugin : BugsnagPlugin {
    companion object {
        var active = false
    }

    override var loaded = false

    override fun loadPlugin(client: Client) {
        active = true
    }

    override fun unloadPlugin() {
        active = false
    }
}

