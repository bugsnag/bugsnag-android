package com.bugsnag.android

internal class AnrPlugin : BugsnagPlugin {
    companion object {
        init {
            BugsnagPluginInterface.registerPlugin(AnrPlugin::class.java)
            System.loadLibrary("bugsnag-android-anr")
        }

        // FIXME right jni name, bytebuffer
        external fun bsg_handler_install_anr(): Boolean
    }

    override fun initialisePlugin(client: Client) {
        bsg_handler_install_anr()
    }
}
