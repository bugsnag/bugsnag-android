package com.bugsnag.android

import com.bugsnag.android.ndk.NativeBridge

internal class NdkPlugin : BugsnagPlugin {

    override fun initialisePlugin(client: Client) {
        System.loadLibrary("bugsnag-ndk")
        val nativeBridge = NativeBridge()
        client.addObserver(nativeBridge)
        client.sendNativeSetupNotification()
    }
}
