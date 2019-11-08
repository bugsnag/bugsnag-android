package com.bugsnag.android.ndk

import android.os.Build
import com.bugsnag.android.Logger
import com.bugsnag.android.NativeInterface
import com.bugsnag.android.StateEvent
import com.bugsnag.android.StateEvent.*
import java.io.File
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
    private val logger: Logger = NativeInterface.getLogger()
    private val reportDirectory: String = NativeInterface.getNativeReportPath()

    private val is32bit: Boolean
        get() {
            val abis = NativeInterface.getCpuAbi()
            return !abis.toList().any { it.contains("64") }
        }

    external fun install(
        reportingDirectory: String,
        autoDetectNdkCrashes: Boolean,
        apiLevel: Int, is32bit: Boolean,
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
    external fun clearBreadcrumbs()
    external fun clearMetadataTab(tab: String)
    external fun removeMetadata(tab: String, key: String)
    external fun pausedSession()
    external fun updateContext(context: String)
    external fun updateInForeground(inForeground: Boolean, activityName: String)
    external fun updateOrientation(orientation: Int)
    external fun updateUserId(newValue: String)
    external fun updateUserEmail(newValue: String)
    external fun updateUserName(newValue: String)

    /**
     * Creates a new native bridge for interacting with native components.
     * Configures logging and ensures that the reporting directory exists
     * immediately.
     */
    init {
        val outFile = File(reportDirectory)
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
            ClearBreadcrumbs -> clearBreadcrumbs()
            is ClearMetadataTab -> clearMetadataTab(msg.section)
            is RemoveMetadata -> removeMetadata(msg.section, msg.key ?: "")
            is AddBreadcrumb -> addBreadcrumb(msg.message, msg.type.toString(), msg.timestamp, msg.metadata)
            NotifyHandled -> addHandledEvent()
            NotifyUnhandled -> addUnhandledEvent()
            PauseSession -> pausedSession()
            is StartSession -> startedSession(msg.id, msg.startedAt, msg.handledCount, msg.unhandledCount)
            is UpdateContext -> updateContext(msg.context ?: "")
            is UpdateInForeground -> updateInForeground(msg.inForeground, msg.contextActivity ?: "")
            is UpdateOrientation -> updateOrientation(msg.orientation)
            is UpdateUserEmail -> updateUserEmail(msg.email ?: "")
            is UpdateUserName -> updateUserName(msg.name ?: "")
            is UpdateUserId -> updateUserId(msg.id ?: "")
        }
    }

    private fun isInvalidMessage(msg: Any?): Boolean {
        if (msg !is StateEvent) {
            return true
        }
        if (!installed.get() && msg !is Install) {
            logger.w("Received message before INSTALL: $msg")
            return true
        }

        logger.i(String.format("Received NDK message %s", msg))
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
                logger.w("Report directory does not exist, cannot read pending reports")
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
            }
            val reportPath = reportDirectory + UUID.randomUUID().toString() + ".crash"
            install(reportPath, arg.autoDetectNdkCrashes, Build.VERSION.SDK_INT, is32bit,
                arg.appVersion ?: "", arg.buildUuid ?: "", arg.releaseStage ?: ""
            )
            installed.set(true)
        } finally {
            lock.unlock()
        }
    }

    private fun handleAddMetadata(arg: AddMetadata) {
        if (arg.key != null) {
            when (val newValue = arg.value) {
                is String -> addMetadataString(arg.section, arg.key!!, newValue)
                is Boolean -> addMetadataBoolean(arg.section, arg.key!!, newValue)
                is Number -> addMetadataDouble(arg.section, arg.key!!, newValue.toDouble())
                else -> Unit
            }
        }
    }
}
