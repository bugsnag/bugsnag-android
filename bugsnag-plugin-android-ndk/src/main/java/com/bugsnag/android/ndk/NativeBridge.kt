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
import java.io.File
import java.nio.charset.Charset
import java.util.Observable
import java.util.Observer
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/**
 * Observes changes in the Bugsnag environment, propagating them to the native layer
 */
class NativeBridge : Observer {

    private val lock = ReentrantLock()
    private val installed = AtomicBoolean(false)
    private val reportDirectory: String = NativeInterface.getNativeReportPath()
    private val logger = NativeInterface.getLogger()

    private val is32bit: Boolean
        get() {
            val abis = NativeInterface.getCpuAbi()
            return !abis.toList().any { it.contains("64") }
        }

    external fun install(
        apiKey: String,
        reportingDirectory: String,
        lastRunInfoPath: String,
        consecutiveLaunchCrashes: Int,
        autoDetectNdkCrashes: Boolean,
        apiLevel: Int,
        is32bit: Boolean,
        appVersion: String,
        buildUuid: String,
        releaseStage: String
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
    external fun getUnwindStackFunction(): Long

    /**
     * Creates a new native bridge for interacting with native components.
     * Configures logging and ensures that the reporting directory exists
     * immediately.
     */
    init {
        val outFile = File(reportDirectory)
        NativeInterface.getLogger()
        if (!outFile.exists() && !outFile.mkdirs()) {
            logger.w("The native reporting directory cannot be created.")
        }
    }

    override fun update(observable: Observable, arg: Any?) {
        if (isInvalidMessage(arg)) return

        when (val msg = arg as StateEvent) {
            is Install -> handleInstallMessage(msg)
            DeliverPending -> deliverPendingReports()
            is AddMetadata -> handleAddMetadata(msg)
            is ClearMetadataSection -> clearMetadataTab(makeSafe(msg.section))
            is ClearMetadataValue -> removeMetadata(
                makeSafe(msg.section),
                makeSafe(msg.key ?: "")
            )
            is AddBreadcrumb -> addBreadcrumb(
                makeSafe(msg.message),
                makeSafe(msg.type.toString()),
                makeSafe(msg.timestamp),
                msg.metadata
            )
            NotifyHandled -> addHandledEvent()
            NotifyUnhandled -> addUnhandledEvent()
            PauseSession -> pausedSession()
            is StartSession -> startedSession(
                makeSafe(msg.id),
                makeSafe(msg.startedAt),
                msg.handledCount,
                msg.unhandledCount
            )
            is UpdateContext -> updateContext(makeSafe(msg.context ?: ""))
            is UpdateInForeground -> updateInForeground(
                msg.inForeground,
                makeSafe(msg.contextActivity ?: "")
            )
            is StateEvent.UpdateLastRunInfo -> updateLastRunInfo(msg.consecutiveLaunchCrashes)
            is StateEvent.UpdateIsLaunching -> updateIsLaunching(msg.isLaunching)
            is UpdateOrientation -> updateOrientation(msg.orientation ?: "")
            is UpdateUser -> {
                updateUserId(makeSafe(msg.user.id ?: ""))
                updateUserName(makeSafe(msg.user.name ?: ""))
                updateUserEmail(makeSafe(msg.user.email ?: ""))
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

    private fun deliverPendingReports() {
        lock.lock()
        try {
            val outDir = File(reportDirectory)
            if (outDir.exists()) {
                val fileList = outDir.listFiles()
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
                    makeSafe(arg.appVersion ?: ""),
                    makeSafe(arg.buildUuid ?: ""),
                    makeSafe(arg.releaseStage ?: "")
                )
                installed.set(true)
            }
        } finally {
            lock.unlock()
        }
    }

    private fun handleAddMetadata(arg: AddMetadata) {
        if (arg.key != null) {
            when (val newValue = arg.value) {
                is String -> addMetadataString(arg.section, arg.key!!, makeSafe(newValue))
                is Boolean -> addMetadataBoolean(arg.section, arg.key!!, newValue)
                is Number -> addMetadataDouble(arg.section, arg.key!!, newValue.toDouble())
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
