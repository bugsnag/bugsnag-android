package com.bugsnag.android

import com.bugsnag.android.ndk.NativeBridge
import java.io.StringWriter
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
        val nativeBridge = NativeBridge(client.bgTaskService)
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
            it.addMetadata("LinkError", "errorClass", error.errorClass)
            it.addMetadata("LinkError", "errorMessage", error.errorMessage)

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

    fun setInternalMetricsEnabled(enabled: Boolean) {
        nativeBridge?.setInternalMetricsEnabled(enabled)
    }

    fun getSignalUnwindStackFunction(): Long {
        return nativeBridge?.getSignalUnwindStackFunction() ?: 0
    }

    fun getCurrentCallbackSetCounts(): Map<String, Int> {
        return nativeBridge?.getCurrentCallbackSetCounts() ?: mapOf()
    }

    fun getCurrentNativeApiCallUsage(): Map<String, Boolean> {
        return nativeBridge?.getCurrentNativeApiCallUsage() ?: mapOf()
    }

    fun initCallbackCounts(counts: Map<String, Int>) {
        nativeBridge?.initCallbackCounts(counts)
    }

    fun notifyAddCallback(callback: String) {
        nativeBridge?.notifyAddCallback(callback)
    }

    fun notifyRemoveCallback(callback: String) {
        nativeBridge?.notifyRemoveCallback(callback)
    }

    fun setStaticData(data: Map<String, Any>) {
        val encoded = StringWriter().apply { use { writer -> JsonStream(writer).use { it.value(data) } } }.toString()
        nativeBridge?.setStaticJsonData(encoded)
    }
}

internal val Client.ndkPlugin: NdkPlugin?
    get() = getPlugin(NdkPlugin::class.java) as NdkPlugin?
