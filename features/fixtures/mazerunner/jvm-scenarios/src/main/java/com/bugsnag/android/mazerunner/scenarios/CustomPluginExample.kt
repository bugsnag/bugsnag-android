package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Client
import com.bugsnag.android.OnErrorCallback
import com.bugsnag.android.Plugin

class CustomPluginExample : Plugin {

    override fun load(client: Client) {
        client.addOnError(
            OnErrorCallback { event ->
                event.context = "Foo Handler Library"
                true
            }
        )
    }

    override fun unload() {}
}
