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

    private fun initNativeBridge(client: Client): NativeBridge {
        val nativeBridge = NativeBridge()
        client.registerObserver(nativeBridge)
        client.setupNdkPlugin()
        return nativeBridge
    }

    override fun load(client: Client) {
        val loaded = loader.loadLibrary("bugsnag-ndk", client) {
            val error = it.errors[0]
            error.errorClass = "NdkLinkError"
            error.errorMessage = LOAD_ERR_MSG
            true
        }

        if (loaded) {
            nativeBridge = initNativeBridge(client)
            enableCrashReporting()
            client.logger.i("Initialised NDK Plugin")
        } else {
            client.logger.e(LOAD_ERR_MSG)
        }
    }

    override fun unload() = disableCrashReporting()

    fun getUnwindStackFunction(): Long {
        val bridge = nativeBridge
        if (bridge != null) {
            return bridge.getUnwindStackFunction()
        }
        return 0
    }
}
