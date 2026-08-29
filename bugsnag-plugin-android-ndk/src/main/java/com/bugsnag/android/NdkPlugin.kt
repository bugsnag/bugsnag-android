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
                client,
                NdkPhaseRecord(
                    phase = PHASE_POST_INIT,
                    phaseStartNs = System.nanoTime(),
                    phaseEndNs = System.nanoTime(),
                    outcome = "error",
                    errorClass = lastLoadErrorClass ?: "UnsatisfiedLinkError"
                )
            )
        }
    }

    private fun performOneTimeSetup(client: Client) {
        var loadErrorClass: String? = null
        var loadErrorMessage: String? = null

        runLibraryResolvePhase(client)

        val loadReport = runLoadLibraryPhase(client) {
            libraryLoader.loadLibraryWithDiagnostics("bugsnag-ndk", client) {
                val error = it.errors[0]
                loadErrorClass = error.errorClass
                loadErrorMessage = error.errorMessage
                lastLoadErrorClass = error.errorClass
                it.addMetadata("LinkError", "errorClass", error.errorClass)
                it.addMetadata("LinkError", "errorMessage", error.errorMessage)

                error.errorClass = "NdkLinkError"
                error.errorMessage = LOAD_ERR_MSG
                true
            }
        }

        val loaded = loadReport.loaded
        if (!loaded) {
            if (loadErrorClass == null) {
                loadErrorClass = loadReport.finalErrorClass
            }
            if (loadErrorMessage == null) {
                loadErrorMessage = loadReport.finalErrorMessage
            }
            if (lastLoadErrorClass == null) {
                lastLoadErrorClass = loadErrorClass
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
                client,
                NdkPhaseRecord(
                    phase = PHASE_LINK_NATIVE,
                    phaseStartNs = System.nanoTime(),
                    phaseEndNs = System.nanoTime(),
                    outcome = "error",
                    errorClass = loadErrorClass ?: "UnsatisfiedLinkError",
                    extraFields = mapOf(
                        "linker_error_message" to loadErrorMessage
                    )
                )
            )
            client.logger.e(LOAD_ERR_MSG)
        }
    }

    private inline fun runPhase(client: Client, phase: String, block: () -> Unit) {
        val phaseStartNs = System.nanoTime()
        try {
            block()
            emitPhaseRecord(
                client,
                NdkPhaseRecord(
                    phase = phase,
                    phaseStartNs = phaseStartNs,
                    phaseEndNs = System.nanoTime(),
                    outcome = "ok",
                    errorClass = null
                )
            )
        } catch (exc: Throwable) {
            emitPhaseRecord(
                client,
                NdkPhaseRecord(
                    phase = phase,
                    phaseStartNs = phaseStartNs,
                    phaseEndNs = System.nanoTime(),
                    outcome = "error",
                    errorClass = exc.javaClass.name
                )
            )
            throw exc
        }
    }

    private fun runLibraryResolvePhase(client: Client) {
        val phaseStartNs = System.nanoTime()
        try {
            val resolution = libraryLoader.resolveLibraryPathDetails("bugsnag-ndk", client)
            emitPhaseRecord(
                client,
                NdkPhaseRecord(
                    phase = PHASE_LIBRARY_RESOLVE,
                    phaseStartNs = phaseStartNs,
                    phaseEndNs = System.nanoTime(),
                    outcome = "ok",
                    errorClass = null,
                    extraFields = mapOf(
                        "resolved_library_path" to resolution.resolvedPath,
                        "resolved_library_mapped_name" to resolution.mappedLibraryName,
                        "resolved_library_path_source" to resolution.pathSource,
                        "resolved_library_path_definitive" to resolution.definitive,
                        "resolved_library_exists" to resolution.fileExists,
                        "resolved_library_size_bytes" to resolution.fileSizeBytes
                    )
                )
            )
        } catch (exc: Throwable) {
            emitPhaseRecord(
                client,
                NdkPhaseRecord(
                    phase = PHASE_LIBRARY_RESOLVE,
                    phaseStartNs = phaseStartNs,
                    phaseEndNs = System.nanoTime(),
                    outcome = "error",
                    errorClass = exc.javaClass.name
                )
            )
            throw exc
        }
    }

    private inline fun runLoadLibraryPhase(
        client: Client,
        block: () -> LibraryLoader.LoadLibraryReport
    ): LibraryLoader.LoadLibraryReport {
        val phaseStartNs = System.nanoTime()
        return try {
            val report = block()
            val loaded = report.loaded
            emitPhaseRecord(
                client,
                NdkPhaseRecord(
                    phase = PHASE_LOAD_LIBRARY,
                    phaseStartNs = phaseStartNs,
                    phaseEndNs = System.nanoTime(),
                    outcome = if (loaded) "ok" else "error",
                    errorClass = if (loaded) null else report.finalErrorClass ?: "UnsatisfiedLinkError",
                    extraFields = mapOf(
                        "queue_wait_ns" to report.queueWaitNs,
                        "native_load_ns" to report.nativeLoadNs,
                        "caller_blocked_ns" to report.callerBlockedNs,
                        "load_retry_attempted" to report.retried,
                        "load_retry_succeeded" to report.retrySucceeded,
                        "first_load_outcome" to when {
                            report.firstErrorClass != null -> "error"
                            loaded -> "ok"
                            else -> "not_attempted"
                        },
                        "second_load_outcome" to when {
                            !report.retried -> "not_attempted"
                            report.retrySucceeded -> "ok"
                            report.secondErrorClass != null -> "error"
                            else -> "unknown"
                        },
                        "linker_error_message" to report.finalErrorMessage,
                        "first_linker_error_class" to report.firstErrorClass,
                        "first_linker_error_message" to report.firstErrorMessage,
                        "second_linker_error_class" to report.secondErrorClass,
                        "second_linker_error_message" to report.secondErrorMessage
                    )
                )
            )
            report
        } catch (exc: Throwable) {
            emitPhaseRecord(
                client,
                NdkPhaseRecord(
                    phase = PHASE_LOAD_LIBRARY,
                    phaseStartNs = phaseStartNs,
                    phaseEndNs = System.nanoTime(),
                    outcome = "error",
                    errorClass = exc.javaClass.name
                )
            )
            throw exc
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

internal data class NdkPhaseRecord(
    val phase: String,
    val phaseStartNs: Long,
    val phaseEndNs: Long,
    val outcome: String,
    val errorClass: String?,
    val extraFields: Map<String, Any?> = emptyMap()
)

internal fun emitPhaseRecord(client: Client, record: NdkPhaseRecord) {
    val thread = java.lang.Thread.currentThread()
    val mainThread = try {
        Looper.getMainLooper()?.thread === thread
    } catch (_: Throwable) {
        false
    }
    val phaseDurationNs = (record.phaseEndNs - record.phaseStartNs).coerceAtLeast(0L)

    val payload = buildString {
        append("{\"phase\":\"")
        append(record.phase)
        append("\",\"phase_start_ns\":")
        append(record.phaseStartNs)
        append(",\"phase_end_ns\":")
        append(record.phaseEndNs)
        append(",\"phase_duration_ns\":")
        append(phaseDurationNs)
        append(",\"thread_name\":\"")
        append(thread.name.escapeJson())
        append("\",\"is_main_thread\":")
        append(mainThread)
        append(",\"outcome\":\"")
        append(record.outcome)
        append("\"")
        if (record.outcome == "error" && record.errorClass != null) {
            append(",\"error_class\":\"")
            append(record.errorClass.escapeJson())
            append("\"")
        }
        record.extraFields.forEach { (key, value) ->
            append(",\"")
            append(key.escapeJson())
            append("\":")
            appendJsonValue(value)
        }
        append("}")
    }

    client.logger.i("NDK load phase diagnostic: $payload")
}

internal fun String.escapeJson(): String {
    return replace("\\", "\\\\").replace("\"", "\\\"")
}

internal fun StringBuilder.appendJsonValue(value: Any?) {
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

internal val Client.ndkPlugin: NdkPlugin?
    get() = getPlugin(NdkPlugin::class.java) as NdkPlugin?
