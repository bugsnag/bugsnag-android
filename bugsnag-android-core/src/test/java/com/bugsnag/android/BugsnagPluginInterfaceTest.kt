package com.bugsnag.android

import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BugsnagPluginInterfaceTest {

    @Mock
    lateinit var client: Client

    @Test
    fun registerPlugin() {
        BugsnagPluginInterface.registerPlugin(FakePlugin::class.java)
        BugsnagPluginInterface.loadPlugins(client)
        assertTrue(FakePlugin.initialised)
    }
}

internal class FakePlugin: BugsnagPlugin {
    companion object {
        var initialised = false
    }

    override fun initialisePlugin(client: Client) {
        initialised = true
    }
}

