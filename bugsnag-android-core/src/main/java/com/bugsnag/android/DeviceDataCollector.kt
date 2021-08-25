package com.bugsnag.android

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import java.io.File
import java.util.Date
import java.util.HashMap
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import android.os.Process as AndroidProcess

internal class DeviceDataCollector(
    private val connectivity: Connectivity,
    private val appContext: Context,
    resources: Resources,
    private val deviceId: String?,
    private val buildInfo: DeviceBuildInfo,
    private val dataDirectory: File,
    rootDetector: RootDetector,
    private val bgTaskService: BackgroundTaskService,
    private val logger: Logger
) {

    private val displayMetrics = resources.displayMetrics
    private val emulator = isEmulator()
    private val screenDensity = getScreenDensity()
    private val dpi = getScreenDensityDpi()
    private val screenResolution = getScreenResolution()
    private val locale = Locale.getDefault().toString()
    private val cpuAbi = getCpuAbi()
    private val runtimeVersions: MutableMap<String, Any>
    private val rootedFuture: Future<Boolean>?
    private val totalMemoryFuture: Future<Long?>? = retrieveTotalDeviceMemory()
    private var orientation = AtomicInteger(resources.configuration.orientation)

    init {
        val map = mutableMapOf<String, Any>()
        buildInfo.apiLevel?.let { map["androidApiLevel"] = it }
        buildInfo.osBuild?.let { map["osBuild"] = it }
        runtimeVersions = map

        rootedFuture = try {
            bgTaskService.submitTask(
                TaskType.IO,
                Callable {
                    rootDetector.isRooted()
                }
            )
        } catch (exc: RejectedExecutionException) {
            logger.w("Failed to perform root detection checks", exc)
            null
        }
    }

    fun generateDevice() = Device(
        buildInfo,
        cpuAbi,
        checkIsRooted(),
        deviceId,
        locale,
        totalMemoryFuture.runCatching { this?.get() }.getOrNull(),
        runtimeVersions.toMutableMap()
    )

    fun generateDeviceWithState(now: Long) = DeviceWithState(
        buildInfo,
        checkIsRooted(),
        deviceId,
        locale,
        totalMemoryFuture.runCatching { this?.get() }.getOrNull(),
        runtimeVersions.toMutableMap(),
        calculateFreeDisk(),
        calculateFreeMemory(),
        getOrientationAsString(),
        Date(now)
    )

    fun getDeviceMetadata(): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        populateBatteryInfo(into = map)
        map["locationStatus"] = getLocationStatus()
        map["networkAccess"] = getNetworkAccess()
        map["brand"] = buildInfo.brand
        map["screenDensity"] = screenDensity
        map["dpi"] = dpi
        map["emulator"] = emulator
        map["screenResolution"] = screenResolution
        return map
    }

    private fun checkIsRooted(): Boolean {
        return try {
            rootedFuture != null && rootedFuture.get()
        } catch (exc: Exception) {
            false
        }
    }

    /**
     * Guesses whether the current device is an emulator or not, erring on the side of caution
     *
     * @return true if the current device is an emulator
     */
    private // genymotion
    fun isEmulator(): Boolean {
        val fingerprint = buildInfo.fingerprint
        return fingerprint != null && (
            fingerprint.startsWith("unknown") ||
                fingerprint.contains("generic") ||
                fingerprint.contains("vbox")
            )
    }

    /**
     * The screen density of the current Android device in dpi, eg. 320
     */
    private fun getScreenDensityDpi(): Int? = displayMetrics?.densityDpi

    /**
     * Populate the current Battery Info into the specified MutableMap
     */
    private fun populateBatteryInfo(into: MutableMap<String, Any?>) {
        try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = appContext.registerReceiverSafe(null, ifilter, logger)

            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra("level", -1)
                val scale = batteryStatus.getIntExtra("scale", -1)

                if (level != -1 || scale != -1) {
                    val batteryLevel: Float = level.toFloat() / scale.toFloat()
                    into["batteryLevel"] = batteryLevel
                }

                val status = batteryStatus.getIntExtra("status", -1)
                val charging =
                    status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                into["charging"] = charging
            }
        } catch (exception: Exception) {
            logger.w("Could not get battery status")
        }
    }

    /**
     * Get the current status of location services
     */
    private fun getLocationStatus(): String? {
        try {
            val cr = appContext.contentResolver
            @Suppress("DEPRECATION") val providersAllowed =
                Settings.Secure.getString(cr, Settings.Secure.LOCATION_PROVIDERS_ALLOWED)
            return when {
                providersAllowed != null && providersAllowed.isNotEmpty() -> "allowed"
                else -> "disallowed"
            }
        } catch (exception: Exception) {
            logger.w("Could not get locationStatus")
        }
        return null
    }

    /**
     * Get the current status of network access, eg "cellular"
     */
    private fun getNetworkAccess(): String = connectivity.retrieveNetworkAccessState()

    /**
     * The screen density scaling factor of the current Android device
     */
    private fun getScreenDensity(): Float? = displayMetrics?.density

    /**
     * The screen resolution of the current Android device in px, eg. 1920x1080
     */
    private fun getScreenResolution(): String? {
        return if (displayMetrics != null) {
            val max = max(displayMetrics.widthPixels, displayMetrics.heightPixels)
            val min = min(displayMetrics.widthPixels, displayMetrics.heightPixels)
            "${max}x$min"
        } else {
            null
        }
    }

    /**
     * Gets information about the CPU / API
     */
    fun getCpuAbi(): Array<String> = buildInfo.cpuAbis ?: emptyArray()

    /**
     * Get the usable disk space on internal storage's data directory
     */
    @SuppressLint("UsableSpace")
    fun calculateFreeDisk(): Long {
        // for this specific case we want the currently usable space, not
        // StorageManager#allocatableBytes() as the UsableSpace lint inspection suggests
        return runCatching {
            bgTaskService.submitTask(
                TaskType.IO,
                Callable { dataDirectory.usableSpace }
            ).get()
        }.getOrDefault(0L)
    }

    /**
     * Get the amount of memory remaining on the device
     */
    private fun calculateFreeMemory(): Long? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val freeMemory = appContext.getActivityManager()
                ?.let { am -> ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) } }
                ?.availMem

            if (freeMemory != null) {
                return freeMemory
            }
        }

        return runCatching {
            @Suppress("PrivateApi")
            AndroidProcess::class.java.getDeclaredMethod("getFreeMemory").invoke(null) as Long?
        }.getOrNull()
    }

    /**
     * Attempt to retrieve the total amount of memory available on the device
     */
    private fun retrieveTotalDeviceMemory(): Future<Long?>? {
        return try {
            bgTaskService.submitTask(
                TaskType.DEFAULT,
                Callable {
                    calculateTotalMemory()
                }
            )
        } catch (exc: RejectedExecutionException) {
            logger.w("Failed to lookup available device memory", exc)
            null
        }
    }

    private fun calculateTotalMemory(): Long? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val totalMemory = appContext.getActivityManager()
                ?.let { am -> ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) } }
                ?.totalMem

            if (totalMemory != null) {
                return totalMemory
            }
        }

        // we try falling back to a reflective API
        return runCatching {
            @Suppress("PrivateApi")
            AndroidProcess::class.java.getDeclaredMethod("getTotalMemory").invoke(null) as Long?
        }.getOrNull()
    }

    /**
     * Get the current device orientation, eg. "landscape"
     */
    internal fun getOrientationAsString(): String? = when (orientation.get()) {
        ORIENTATION_LANDSCAPE -> "landscape"
        ORIENTATION_PORTRAIT -> "portrait"
        else -> null
    }

    /**
     * Called whenever the orientation is updated so that the device information is accurate.
     * Currently this is only invoked by [ClientComponentCallbacks]. Returns true if the
     * orientation has changed, otherwise false.
     */
    internal fun updateOrientation(newOrientation: Int): Boolean {
        return orientation.getAndSet(newOrientation) != newOrientation
    }

    fun addRuntimeVersionInfo(key: String, value: String) {
        runtimeVersions[key] = value
    }
}
