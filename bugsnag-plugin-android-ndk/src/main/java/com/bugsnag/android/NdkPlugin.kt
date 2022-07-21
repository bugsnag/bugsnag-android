package com.bugsnag.android

import com.bugsnag.android.ndk.NativeBridge
import java.util.concurrent.atomic.AtomicBoolean

internal class NdkPlugin : Plugin {

    private companion object {
        private const val LOAD_ERR_MSG = "Native library could not be linked. Bugsnag will " +
            "not report NDK errors. See https://docs.bugsnag.com/platforms/android/ndk-link-errors"
    }

    private val libraryLoader = LibraryLoader()
    private val oneTimeSetupPerformed = AtomicBoolean(false)

    private external fun enableCrashReporting()
    private external fun disableCrashReporting()

    private external fun getBinaryArch(): String

    private var client: Client? = null

    var nativeBridge: NativeBridge? = null
        private set

    private fun initNativeBridge(client: Client): NativeBridge {
        val nativeBridge = NativeBridge()
        client.addObserver(nativeBridge)
        client.setupNdkPlugin()
        return nativeBridge
    }

    override fun load(client: Client) {
        this.client = client

        if (!oneTimeSetupPerformed.getAndSet(true)) {
            performOneTimeSetup(client)
        }
        if (libraryLoader.isLoaded) {
            enableCrashReporting()
            client.logger.i("Initialised NDK Plugin")
        }
    }

    private fun performOneTimeSetup(client: Client) {
        libraryLoader.loadLibrary("bugsnag-ndk", client) {
            val error = it.errors[0]
            error.errorClass = "NdkLinkError"
            error.errorMessage = LOAD_ERR_MSG
            true
        }
        if (libraryLoader.isLoaded) {
            client.setBinaryArch(getBinaryArch())
            nativeBridge = initNativeBridge(client)
        } else {
            client.logger.e(LOAD_ERR_MSG)
        }
    }

    override fun unload() {
        if (libraryLoader.isLoaded) {
            disableCrashReporting()
            nativeBridge?.let { bridge ->
                client?.removeObserver(bridge)
            }
        }
    }

    fun getSignalUnwindStackFunction(): Long {
        val bridge = nativeBridge
        if (bridge != null) {
            return bridge.getSignalUnwindStackFunction()
        }
        return 0
    }

    fun getCalledNativeFunctions(): List<Long> {
        val bridge = nativeBridge
        if (bridge != null) {
            return bridge.getCalledNativeFunctions()
        }
        return listOf()
    }
}

internal val Client.ndkPlugin: NdkPlugin?
    get() = getPlugin(NdkPlugin::class.java) as NdkPlugin?
