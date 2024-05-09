package com.bugsnag.android.ndk

import android.os.Build
import com.bugsnag.android.BreadcrumbType
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
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.StateObserver
import com.bugsnag.android.internal.TaskType
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Observes changes in the Bugsnag environment, propagating them to the native layer
 */
class NativeBridge(private val bgTaskService: BackgroundTaskService) : StateObserver {

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
        eventUUID: String,
        consecutiveLaunchCrashes: Int,
        autoDetectNdkCrashes: Boolean,
        apiLevel: Int,
        is32bit: Boolean,
        threadSendPolicy: Int,
        maxBreadcrumbs: Int,
    )

    external fun startedSession(
        sessionID: String,
        key: String,
        handledCount: Int,
        unhandledCount: Int
    )

    fun addBreadcrumb(name: String, type: String, timestamp: String, metadata: Any) {
        val breadcrumbType = BreadcrumbType.values()
            .find { it.toString() == type }
            ?: BreadcrumbType.MANUAL

        addBreadcrumb(name, breadcrumbType.toNativeValue(), timestamp, metadata)
    }

    private external fun addBreadcrumb(name: String, type: Int, timestamp: String, metadata: Any)
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
    external fun getCurrentCallbackSetCounts(): Map<String, Int>?
    external fun getCurrentNativeApiCallUsage(): Map<String, Boolean>?
    external fun setStaticJsonData(data: String)
    external fun setInternalMetricsEnabled(enabled: Boolean)

    override fun onStateChange(event: StateEvent) {
        if (isInvalidMessage(event)) return

        when (event) {
            is Install -> handleInstallMessage(event)
            is DeliverPending -> deliverPendingReports()
            is AddMetadata -> handleAddMetadata(event)
            is ClearMetadataSection -> clearMetadataTab(event.section)
            is ClearMetadataValue -> removeMetadata(
                event.section,
                event.key ?: ""
            )

            is AddBreadcrumb -> addBreadcrumb(
                event.message,
                event.type.toNativeValue(),
                event.timestamp,
                event.metadata
            )

            NotifyHandled -> addHandledEvent()
            NotifyUnhandled -> addUnhandledEvent()
            PauseSession -> pausedSession()
            is StartSession -> startedSession(
                event.id,
                event.startedAt,
                event.handledCount,
                event.unhandledCount
            )

            is UpdateContext -> updateContext(event.context ?: "")
            is UpdateInForeground -> updateInForeground(
                event.inForeground,
                event.contextActivity ?: ""
            )

            is StateEvent.UpdateLastRunInfo -> updateLastRunInfo(event.consecutiveLaunchCrashes)
            is StateEvent.UpdateIsLaunching -> {
                updateIsLaunching(event.isLaunching)

                if (!event.isLaunching) {
                    // we refreshSymbolTable on the background to avoid holding up the main thread
                    bgTaskService.submitTask(TaskType.DEFAULT, this::refreshSymbolTable)
                }
            }

            is UpdateOrientation -> updateOrientation(event.orientation ?: "")
            is UpdateUser -> {
                updateUserId(event.user.id ?: "")
                updateUserName(event.user.name ?: "")
                updateUserEmail(event.user.email ?: "")
            }

            is StateEvent.UpdateMemoryTrimEvent -> updateLowMemory(
                event.isLowMemory,
                event.memoryTrimLevelDescription
            )

            is StateEvent.AddFeatureFlag -> addFeatureFlag(
                event.name,
                event.variant
            )

            is StateEvent.ClearFeatureFlag -> clearFeatureFlag(event.name)
            is StateEvent.ClearFeatureFlags -> clearFeatureFlags()
        }
    }

    private fun deliverPendingReports() {
        val discardScanner = ReportDiscardScanner(logger)
        reportDirectory.listFiles()?.forEach { reportFile ->
            if (discardScanner.shouldDiscard(reportFile)) {
                reportFile.delete()
            } else {
                NativeInterface.deliverReport(reportFile)
            }
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

    private fun handleInstallMessage(arg: Install) {
        lock.withLock {
            if (installed.get()) {
                logger.w("Received duplicate setup message with arg: $arg")
            } else {
                install(
                    arg.apiKey,
                    reportDirectory.absolutePath,
                    arg.lastRunInfoPath,
                    UUID.randomUUID().toString(),
                    arg.consecutiveLaunchCrashes,
                    arg.autoDetectNdkCrashes,
                    Build.VERSION.SDK_INT,
                    is32bit,
                    arg.sendThreads.ordinal,
                    arg.maxBreadcrumbs
                )
                installed.set(true)
            }
        }
    }

    private fun handleAddMetadata(arg: AddMetadata) {
        if (arg.key != null) {
            when (val newValue = OpaqueValue.makeSafe(arg.value)) {
                is String -> addMetadataString(arg.section, arg.key!!, newValue)
                is Boolean -> addMetadataBoolean(arg.section, arg.key!!, newValue)
                is Number -> addMetadataDouble(arg.section, arg.key!!, newValue.toDouble())
                is OpaqueValue -> addMetadataOpaque(arg.section, arg.key!!, newValue.json)
                else -> Unit
            }
        }
    }

    /**
     * Convert a [BreadcrumbType] to the value expected by the [addBreadcrumb] implementation. This
     * is implemented as an exhaustive when so that any changes to the `enum` are picked up and/or
     * don't directly impact the implementation.
     */
    @Suppress("MagicNumber") // introducing consts would reduce readability
    private fun BreadcrumbType.toNativeValue(): Int = when (this) {
        BreadcrumbType.ERROR -> 0
        BreadcrumbType.LOG -> 1
        BreadcrumbType.MANUAL -> 2
        BreadcrumbType.NAVIGATION -> 3
        BreadcrumbType.PROCESS -> 4
        BreadcrumbType.REQUEST -> 5
        BreadcrumbType.STATE -> 6
        BreadcrumbType.USER -> 7
    }
}
