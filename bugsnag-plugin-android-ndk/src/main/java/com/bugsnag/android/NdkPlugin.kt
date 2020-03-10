package com.bugsnag.android

import com.bugsnag.android.ndk.NativeBridge

internal class NdkPlugin : Plugin {

    companion object {
        init {
            System.loadLibrary("bugsnag-ndk")
        }
    }

    private external fun enableCrashReporting()
    private external fun disableCrashReporting()

    private var nativeBridge: NativeBridge? = null

    override fun load(client: Client) {
        if (nativeBridge == null) {
            nativeBridge = NativeBridge()
            client.registerObserver(nativeBridge)
            client.sendNativeSetupNotification()
            client.syncInitialState()
        }
        enableCrashReporting()
        client.logger.i("Initialised NDK Plugin")
    }

    override fun unload() = disableCrashReporting()
}
