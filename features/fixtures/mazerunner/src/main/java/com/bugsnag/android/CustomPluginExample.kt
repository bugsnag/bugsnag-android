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
        client.config.beforeSend { report ->
            report.notifier.name = "Foo Handler Library"
            report.notifier.version = "2.1.0"
            report.notifier.setURL("https://example.com")

            true
        }
    }

    override fun unloadPlugin() {
    }
}
