package com.bugsnag.android

import android.os.Looper
import com.bugsnag.android.ndk.NativeBridge
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicBoolean

internal class NdkPlugin : Plugin {

    private companion object {
        private const val LOAD_ERR_MSG = "Native library could not be linked. Bugsnag will " +
            "not report NDK errors. See https://docs.bugsnag.com/platforms/android/ndk-link-errors"

        private const val PHASE_PLUGIN_INIT = "plugin_init"
        private const val PHASE_LIBRARY_RESOLVE = "library_resolve"
        private const val PHASE_LOAD_LIBRARY = "load_library"
        private const val PHASE_LINK_NATIVE = "link_native"
        private const val PHASE_POST_INIT = "post_init"
    }

    private val libraryLoader = LibraryLoader()
    private val oneTimeSetupPerformed = AtomicBoolean(false)
    private var lastLoadErrorClass: String? = null

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
        runPhase(client, PHASE_PLUGIN_INIT) {
            this.client = client
            if (!oneTimeSetupPerformed.getAndSet(true)) {
                performOneTimeSetup(client)
            }
        }

        if (libraryLoader.isLoaded) {
            runPhase(client, PHASE_POST_INIT) {
                enableCrashReporting()
                client.logger.i("Initialised NDK Plugin")
            }
        } else {
            emitPhaseRecord(
                client = client,
                phase = PHASE_POST_INIT,
                phaseStartNs = System.nanoTime(),
                phaseEndNs = System.nanoTime(),
                outcome = "error",
                errorClass = lastLoadErrorClass ?: "UnsatisfiedLinkError"
            )
        }
    }

    private fun performOneTimeSetup(client: Client) {
        var loadErrorClass: String? = null

        runLibraryResolvePhase(client)

        val loaded = runLoadLibraryPhase(client, resolveErrorClass = { loadErrorClass }) {
            libraryLoader.loadLibrary("bugsnag-ndk", client) {
                val error = it.errors[0]
                loadErrorClass = error.errorClass
                lastLoadErrorClass = error.errorClass
                it.addMetadata("LinkError", "errorClass", error.errorClass)
                it.addMetadata("LinkError", "errorMessage", error.errorMessage)

                error.errorClass = "NdkLinkError"
                error.errorMessage = LOAD_ERR_MSG
                true
            }
        }

        if (loaded) {
            lastLoadErrorClass = null
            runPhase(client, PHASE_LINK_NATIVE) {
                client.setBinaryArch(getBinaryArch())
                nativeBridge = initNativeBridge(client)
            }
        } else {
            emitPhaseRecord(
                client = client,
                phase = PHASE_LINK_NATIVE,
                phaseStartNs = System.nanoTime(),
                phaseEndNs = System.nanoTime(),
                outcome = "error",
                errorClass = loadErrorClass ?: "UnsatisfiedLinkError"
            )
            client.logger.e(LOAD_ERR_MSG)
        }
    }

    private inline fun runPhase(client: Client, phase: String, block: () -> Unit) {
        val phaseStartNs = System.nanoTime()
        try {
            block()
            emitPhaseRecord(client, phase, phaseStartNs, System.nanoTime(), "ok", null)
        } catch (exc: Throwable) {
            emitPhaseRecord(
                client,
                phase,
                phaseStartNs,
                System.nanoTime(),
                "error",
                exc.javaClass.name
            )
            throw exc
        }
    }

    private fun runLibraryResolvePhase(client: Client) {
        val phaseStartNs = System.nanoTime()
        try {
            val resolvedPath = libraryLoader.resolveLibraryPath("bugsnag-ndk", client)
            val resolvedExists = java.io.File(resolvedPath).exists()
            emitPhaseRecord(
                client = client,
                phase = PHASE_LIBRARY_RESOLVE,
                phaseStartNs = phaseStartNs,
                phaseEndNs = System.nanoTime(),
                outcome = "ok",
                errorClass = null,
                extraFields = mapOf(
                    "resolved_library_path" to resolvedPath,
                    "resolved_library_exists" to resolvedExists
                )
            )
        } catch (exc: Throwable) {
            emitPhaseRecord(
                client = client,
                phase = PHASE_LIBRARY_RESOLVE,
                phaseStartNs = phaseStartNs,
                phaseEndNs = System.nanoTime(),
                outcome = "error",
                errorClass = exc.javaClass.name
            )
            throw exc
        }
    }

    private inline fun runLoadLibraryPhase(
        client: Client,
        resolveErrorClass: () -> String?,
        block: () -> Boolean
    ): Boolean {
        val phaseStartNs = System.nanoTime()
        return try {
            val loaded = block()
            emitPhaseRecord(
                client = client,
                phase = PHASE_LOAD_LIBRARY,
                phaseStartNs = phaseStartNs,
                phaseEndNs = System.nanoTime(),
                outcome = if (loaded) "ok" else "error",
                errorClass = if (loaded) null else resolveErrorClass() ?: "UnsatisfiedLinkError"
            )
            loaded
        } catch (exc: Throwable) {
            emitPhaseRecord(
                client,
                PHASE_LOAD_LIBRARY,
                phaseStartNs,
                System.nanoTime(),
                "error",
                exc.javaClass.name
            )
            throw exc
        }
    }

    private fun emitPhaseRecord(
        client: Client,
        phase: String,
        phaseStartNs: Long,
        phaseEndNs: Long,
        outcome: String,
        errorClass: String?,
        extraFields: Map<String, Any?> = emptyMap()
    ) {
        val thread = java.lang.Thread.currentThread()
        val mainThread = try {
            Looper.getMainLooper()?.thread === thread
        } catch (_: Throwable) {
            false
        }
        val phaseDurationNs = (phaseEndNs - phaseStartNs).coerceAtLeast(0L)

        val payload = buildString {
            append("{\"phase\":\"")
            append(phase)
            append("\",\"phase_start_ns\":")
            append(phaseStartNs)
            append(",\"phase_end_ns\":")
            append(phaseEndNs)
            append(",\"phase_duration_ns\":")
            append(phaseDurationNs)
            append(",\"thread_name\":\"")
            append(thread.name.escapeJson())
            append("\",\"is_main_thread\":")
            append(mainThread)
            append(",\"outcome\":\"")
            append(outcome)
            append("\"")
            if (outcome == "error" && errorClass != null) {
                append(",\"error_class\":\"")
                append(errorClass.escapeJson())
                append("\"")
            }
            extraFields.forEach { (key, value) ->
                append(",\"")
                append(key.escapeJson())
                append("\":")
                appendJsonValue(value)
            }
            append("}")
        }

        client.logger.i("NDK load phase diagnostic: $payload")
    }

    private fun String.escapeJson(): String {
        return replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private fun StringBuilder.appendJsonValue(value: Any?) {
        when (value) {
            null -> append("null")
            is Number, is Boolean -> append(value)
            else -> {
                append("\"")
                append(value.toString().escapeJson())
                append("\"")
            }
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

    // Called via reflection from NdkPluginCaller and AnrPlugin; do not remove
    @Suppress("unused")
    fun setInternalMetricsEnabled(enabled: Boolean) {
        nativeBridge?.setInternalMetricsEnabled(enabled)
    }

    // Called via reflection from AnrPlugin; do not remove
    @Suppress("unused")
    fun getSignalUnwindStackFunction(): Long {
        return nativeBridge?.getSignalUnwindStackFunction() ?: 0
    }

    // Called via reflection from NdkPluginCaller; do not remove
    @Suppress("unused")
    fun getCurrentCallbackSetCounts(): Map<String, Int> {
        return nativeBridge?.getCurrentCallbackSetCounts() ?: mapOf()
    }

    // Called via reflection from NdkPluginCaller; do not remove
    @Suppress("unused")
    fun getCurrentNativeApiCallUsage(): Map<String, Boolean> {
        return nativeBridge?.getCurrentNativeApiCallUsage() ?: mapOf()
    }

    // Called via reflection from NdkPluginCaller; do not remove
    @Suppress("unused")
    fun initCallbackCounts(counts: Map<String, Int>) {
        nativeBridge?.initCallbackCounts(counts)
    }

    // Called via reflection from NdkPluginCaller; do not remove
    @Suppress("unused")
    fun notifyAddCallback(callback: String) {
        nativeBridge?.notifyAddCallback(callback)
    }

    // Called via reflection from NdkPluginCaller; do not remove
    @Suppress("unused")
    fun notifyRemoveCallback(callback: String) {
        nativeBridge?.notifyRemoveCallback(callback)
    }

    // Called via reflection from NdkPluginCaller; do not remove
    @Suppress("unused")
    fun setStaticData(data: Map<String, Any>) {
        val encoded = StringWriter().apply { use { writer -> JsonStream(writer).use { it.value(data) } } }.toString()
        nativeBridge?.setStaticJsonData(encoded)
    }
}

internal val Client.ndkPlugin: NdkPlugin?
    get() = getPlugin(NdkPlugin::class.java) as NdkPlugin?
