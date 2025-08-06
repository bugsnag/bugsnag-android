package com.bugsnag.android

import android.app.Application

class BugsnagNavigationPlugin : Plugin {

    override fun load(client: Client) {
        val application = client.appContext as Application

        application.registerActivityLifecycleCallbacks(
            NavControllerAutomation(),
        )
    }

    override fun unload() = Unit
}
