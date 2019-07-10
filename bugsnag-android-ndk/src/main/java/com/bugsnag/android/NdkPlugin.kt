package com.bugsnag.android

import com.bugsnag.android.ndk.NativeBridge

internal class NdkPlugin : BugsnagPlugin {
    companion object {
        init {
            BugsnagPluginInterface.registerPlugin(NdkPlugin::class.java)
        }
    }

    override fun initialisePlugin(client: Client) {
        try {
            val nativeBridge = NativeBridge()
            client.addObserver(nativeBridge)
        } catch (exception: ClassNotFoundException) {
            // ignore this one, will happen if the NDK plugin is not present
            Logger.info("Bugsnag NDK integration not available")
        } catch (exception: InstantiationException) {
            Logger.warn("Failed to instantiate NDK observer", exception)
        } catch (exception: IllegalAccessException) {
            Logger.warn("Could not access NDK observer", exception)
        }
        client.sendNativeSetupNotification()
    }
}
