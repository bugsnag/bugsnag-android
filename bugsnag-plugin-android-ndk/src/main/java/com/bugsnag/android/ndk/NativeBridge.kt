package com.bugsnag.android.ndk

import android.os.Build
import com.bugsnag.android.NativeInterface
import com.bugsnag.android.StateEvent
import com.bugsnag.android.StateEvent.AddBreadcrumb
import com.bugsnag.android.StateEvent.AddMetadata
import com.bugsnag.android.StateEvent.ClearMetadataSection
import com.bugsnag.android.StateEvent.ClearMetadataValue
import com.bugsnag.android.StateEvent.DeliverPending
import com.bugsnag.android.StateEvent.Install
import com.bugsnag.android.StateEvent.NotifyHandled
import com.bugsnag.android.StateEvent.NotifyUnhandled
import com.bugsnag.android.StateEvent.PauseSession
import com.bugsnag.android.StateEvent.StartSession
import com.bugsnag.android.StateEvent.UpdateContext
import com.bugsnag.android.StateEvent.UpdateInForeground
import com.bugsnag.android.StateEvent.UpdateOrientation
import com.bugsnag.android.StateEvent.UpdateUser
import com.bugsnag.android.internal.StateObserver
import java.io.File
import java.nio.charset.Charset
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/**
 * Observes changes in the Bugsnag environment, propagating them to the native layer
 */
class NativeBridge : StateObserver {

    private val lock = ReentrantLock()
    private val installed = AtomicBoolean(false)
    private val reportDirectory: File = NativeInterface.getNativeReportPath()
    private val logger = NativeInterface.getLogger()

    private val is32bit: Boolean
        get() {
            val abis = NativeInterface.getCpuAbi()
            return !abis.any { it.contains("64") }
        }

    external fun install(
        apiKey: String,
        reportingDirectory: String,
        lastRunInfoPath: String,
        consecutiveLaunchCrashes: Int,
        autoDetectNdkCrashes: Boolean,
        apiLevel: Int,
        is32bit: Boolean,
        threadSendPolicy: Int
    )

    external fun startedSession(
        sessionID: String,
        key: String,
        handledCount: Int,
        unhandledCount: Int
    )

    external fun deliverReportAtPath(filePath: String)
    external fun addBreadcrumb(name: String, type: String, timestamp: String, metadata: Any)
    external fun addMetadataString(tab: String, key: String, value: String)
    external fun addMetadataDouble(tab: String, key: String, value: Double)
    external fun addMetadataBoolean(tab: String, key: String, value: Boolean)
    external fun addMetadataOpaque(tab: String, key: String, value: String)
    external fun addHandledEvent()
    external fun addUnhandledEvent()
    external fun clearMetadataTab(tab: String)
    external fun removeMetadata(tab: String, key: String)
    external fun pausedSession()
    external fun updateContext(context: String)
    external fun updateInForeground(inForeground: Boolean, activityName: String)
    external fun updateIsLaunching(isLaunching: Boolean)
    external fun updateLastRunInfo(consecutiveLaunchCrashes: Int)
    external fun updateOrientation(orientation: String)
    external fun updateUserId(newValue: String)
    external fun updateUserEmail(newValue: String)
    external fun updateUserName(newValue: String)
    external fun getSignalUnwindStackFunction(): Long
    external fun updateLowMemory(newValue: Boolean, memoryTrimLevelDescription: String)
    external fun addFeatureFlag(name: String, variant: String?)
    external fun clearFeatureFlag(name: String)
    external fun clearFeatureFlags()
    external fun refreshSymbolTable()
    external fun initCallbackCounts(counts: Map<String, Int>)
    external fun notifyAddCallback(callback: String)
    external fun notifyRemoveCallback(callback: String)
    external fun getCurrentCallbackSetCounts(): Map<String, Int>
    external fun getCurrentNativeApiCallUsage(): Map<String, Boolean>
    external fun setStaticJsonData(data: String)
    external fun setInternalMetricsEnabled(enabled: Boolean)

    override fun onStateChange(event: StateEvent) {
        if (isInvalidMessage(event)) return

        when (event) {
            is Install -> handleInstallMessage(event)
            DeliverPending -> deliverPendingReports()
            is AddMetadata -> handleAddMetadata(event)
            is ClearMetadataSection -> clearMetadataTab(makeSafe(event.section))
            is ClearMetadataValue -> removeMetadata(
                makeSafe(event.section),
                makeSafe(event.key ?: "")
            )
            is AddBreadcrumb -> addBreadcrumb(
                makeSafe(event.message),
                makeSafe(event.type.toString()),
                makeSafe(event.timestamp),
                makeSafeMetadata(event.metadata)
            )
            NotifyHandled -> addHandledEvent()
            NotifyUnhandled -> addUnhandledEvent()
            PauseSession -> pausedSession()
            is StartSession -> startedSession(
                makeSafe(event.id),
                makeSafe(event.startedAt),
                event.handledCount,
                event.unhandledCount
            )
            is UpdateContext -> updateContext(makeSafe(event.context ?: ""))
            is UpdateInForeground -> updateInForeground(
                event.inForeground,
                makeSafe(event.contextActivity ?: "")
            )
            is StateEvent.UpdateLastRunInfo -> updateLastRunInfo(event.consecutiveLaunchCrashes)
            is StateEvent.UpdateIsLaunching -> updateIsLaunching(event.isLaunching)
            is UpdateOrientation -> updateOrientation(event.orientation ?: "")
            is UpdateUser -> {
                updateUserId(makeSafe(event.user.id ?: ""))
                updateUserName(makeSafe(event.user.name ?: ""))
                updateUserEmail(makeSafe(event.user.email ?: ""))
            }
            is StateEvent.UpdateMemoryTrimEvent -> updateLowMemory(
                event.isLowMemory,
                event.memoryTrimLevelDescription
            )
            is StateEvent.AddFeatureFlag -> addFeatureFlag(
                makeSafe(event.name),
                event.variant?.let { makeSafe(it) }
            )
            is StateEvent.ClearFeatureFlag -> clearFeatureFlag(makeSafe(event.name))
            is StateEvent.ClearFeatureFlags -> clearFeatureFlags()
        }
    }

    private fun makeSafeMetadata(metadata: Map<String, Any?>): Map<String, Any?> {
        if (metadata.isEmpty()) return metadata
        return object : Map<String, Any?> by metadata {
            override fun get(key: String): Any? = OpaqueValue.makeSafe(metadata[key])
        }
    }

    private fun isInvalidMessage(msg: Any?): Boolean {
        if (msg == null || msg !is StateEvent) {
            return true
        }
        if (!installed.get() && msg !is Install) {
            logger.w("Received message before INSTALL: $msg")
            return true
        }
        return false
    }

    private fun deliverPendingReports() {
        lock.lock()
        val filenameRegex = """.*\.crash$""".toRegex()
        try {
            val outDir = reportDirectory
            if (outDir.exists()) {
                val fileList = outDir.listFiles()?.filter {
                    filenameRegex.containsMatchIn(it.name)
                }
                if (fileList != null) {
                    for (file in fileList) {
                        deliverReportAtPath(file.absolutePath)
                    }
                }
            } else {
                logger.w("Payload directory does not exist, cannot read pending reports")
            }
        } catch (ex: Exception) {
            logger.w("Failed to parse/write pending reports: $ex")
        } finally {
            lock.unlock()
        }
    }

    private fun handleInstallMessage(arg: Install) {
        lock.lock()
        try {
            if (installed.get()) {
                logger.w("Received duplicate setup message with arg: $arg")
            } else {
                val reportPath = File(reportDirectory, "${UUID.randomUUID()}.crash").absolutePath
                install(
                    makeSafe(arg.apiKey),
                    reportPath,
                    makeSafe(arg.lastRunInfoPath),
                    arg.consecutiveLaunchCrashes,
                    arg.autoDetectNdkCrashes,
                    Build.VERSION.SDK_INT,
                    is32bit,
                    arg.sendThreads.ordinal
                )
                installed.set(true)
            }
        } finally {
            lock.unlock()
        }
    }

    private fun handleAddMetadata(arg: AddMetadata) {
        if (arg.key != null) {
            when (val newValue = OpaqueValue.makeSafe(arg.value)) {
                is String -> addMetadataString(arg.section, arg.key!!, makeSafe(newValue))
                is Boolean -> addMetadataBoolean(arg.section, arg.key!!, newValue)
                is Number -> addMetadataDouble(arg.section, arg.key!!, newValue.toDouble())
                is OpaqueValue -> addMetadataOpaque(arg.section, arg.key!!, newValue.json)
                else -> Unit
            }
        }
    }

    /**
     * Ensure the string is safe to be passed to native layer by forcing the encoding
     * to UTF-8.
     */
    private fun makeSafe(text: String): String {
        // The Android platform default charset is always UTF-8
        return String(text.toByteArray(Charset.defaultCharset()))
    }
}
