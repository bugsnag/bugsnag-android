package com.bugsnag.android

import com.bugsnag.android.ndk.NativeBridge

internal class NdkPlugin : Plugin {

    private companion object {
        private const val LOAD_ERR_MSG = "Native library could not be linked. Bugsnag will " +
            "not report NDK errors. See https://docs.bugsnag.com/platforms/android/ndk-link-errors"
    }

    private val loader = LibraryLoader()

    private external fun enableCrashReporting()
    private external fun disableCrashReporting()

    private var nativeBridge: NativeBridge? = null

    override fun load(client: Client) {
        val loaded = loader.loadLibrary("bugsnag-ndk", client) {
            val error = it.errors[0]
            error.errorClass = "NdkLinkError"
            error.errorMessage = LOAD_ERR_MSG
            true
        }

        if (loaded) {
            if (nativeBridge == null) {
                nativeBridge = NativeBridge()
                client.registerObserver(nativeBridge)
                client.sendNativeSetupNotification()
                client.syncInitialState()
            }
            enableCrashReporting()
            client.logger.i("Initialised NDK Plugin")
        } else {
            client.logger.e(LOAD_ERR_MSG)
        }
    }

    override fun unload() = disableCrashReporting()
}
