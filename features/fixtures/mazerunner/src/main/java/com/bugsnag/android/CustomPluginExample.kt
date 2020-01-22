package com.bugsnag.android

// In the main package as a part of an internal interface

class CustomPluginExample : BugsnagPlugin {

    override var loaded = false


    companion object {
        @JvmStatic
        fun register() {
            BugsnagPluginInterface.registerPlugin(CustomPluginExample::class.java)
        }
    }

    override fun loadPlugin(client: Client) {
        client.addOnError(OnErrorCallback { event ->
            event.context = "Foo Handler Library"
            true
        })
    }

    override fun unloadPlugin() {
    }
}
