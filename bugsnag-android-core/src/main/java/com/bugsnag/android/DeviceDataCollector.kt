package com.bugsnag.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import android.os.BatteryManager
import android.provider.Settings
import java.io.File
import java.util.Date
import java.util.HashMap
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

internal class DeviceDataCollector(
    private val connectivity: Connectivity,
    private val appContext: Context,
    private val resources: Resources?,
    private val installId: String,
    private val buildInfo: DeviceBuildInfo,
    private val dataDirectory: File,
    private val logger: Logger
) {

    private val displayMetrics = resources?.displayMetrics
    private val emulator = isEmulator()
    private val rooted = isRooted()
    private val screenDensity = getScreenDensity()
    private val dpi = getScreenDensityDpi()
    private val screenResolution = getScreenResolution()
    private val locale = Locale.getDefault().toString()
    private val cpuAbi = getCpuAbi()
    private val runtimeVersions: MutableMap<String, Any>

    init {
        val map = mutableMapOf<String, Any>()
        buildInfo.apiLevel?.let { map["androidApiLevel"] = it }
        buildInfo.osBuild?.let { map["osBuild"] = it }
        runtimeVersions = map
    }

    fun generateDevice() = Device(
        buildInfo,
        cpuAbi,
        rooted,
        installId,
        locale,
        calculateTotalMemory(),
        runtimeVersions.toMutableMap()
    )

    fun generateDeviceWithState(now: Long) = DeviceWithState(
        buildInfo,
        rooted,
        installId,
        locale,
        calculateTotalMemory(),
        runtimeVersions.toMutableMap(),
        calculateFreeDisk(),
        calculateFreeMemory(),
        calculateOrientation(),
        Date(now)
    )

    fun getDeviceMetadata(): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        map["batteryLevel"] = getBatteryLevel()
        map["charging"] = isCharging()
        map["locationStatus"] = getLocationStatus()
        map["networkAccess"] = getNetworkAccess()
        map["brand"] = buildInfo.brand
        map["screenDensity"] = screenDensity
        map["dpi"] = dpi
        map["emulator"] = emulator
        map["screenResolution"] = screenResolution
        return map
    }

    /**
     * Check if the current Android device is rooted
     */
    private fun isRooted(): Boolean {
        val tags = buildInfo.tags
        if (tags != null && tags.contains("test-keys")) {
            return true
        }

        runCatching {
            for (candidate in ROOT_INDICATORS) {
                if (File(candidate).exists()) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Guesses whether the current device is an emulator or not, erring on the side of caution
     *
     * @return true if the current device is an emulator
     */
    private// genymotion
    fun isEmulator(): Boolean {
        val fingerprint = buildInfo.fingerprint
        return fingerprint != null && (fingerprint.startsWith("unknown")
                || fingerprint.contains("generic")
                || fingerprint.contains("vbox"))
    }

    /**
     * The screen density of the current Android device in dpi, eg. 320
     */
    private fun getScreenDensityDpi(): Int? = displayMetrics?.densityDpi

    /**
     * Get the current battery charge level, eg 0.3
     */
    private fun getBatteryLevel(): Float? {
        try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = appContext.registerReceiver(null, ifilter)

            if (batteryStatus != null) {
                return batteryStatus.getIntExtra(
                    "level",
                    -1
                ) / batteryStatus.getIntExtra("scale", -1).toFloat()
            }
        } catch (exception: Exception) {
            logger.w("Could not get batteryLevel")
        }
        return null
    }

    /**
     * Is the device currently charging/full battery?
     */
    private fun isCharging(): Boolean? {
        try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = appContext.registerReceiver(null, ifilter)

            if (batteryStatus != null) {
                val status = batteryStatus.getIntExtra("status", -1)
                return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }
        } catch (exception: Exception) {
            logger.w("Could not get charging status")
        }
        return null
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
            String.format(Locale.US, "%dx%d", max, min)
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
        return dataDirectory.usableSpace
    }

    /**
     * Get the amount of memory remaining that the VM can allocate
     */
    private fun calculateFreeMemory(): Long {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()

        return if (maxMemory != Long.MAX_VALUE) {
            maxMemory - runtime.totalMemory() + runtime.freeMemory()
        } else {
            runtime.freeMemory()
        }
    }

    /**
     * Get the total memory available on the current Android device, in bytes
     */
    private fun calculateTotalMemory(): Long {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        return when {
            maxMemory != Long.MAX_VALUE -> maxMemory
            else -> runtime.totalMemory()
        }
    }

    /**
     * Get the device orientation, eg. "landscape"
     */
    internal fun calculateOrientation() = when (resources?.configuration?.orientation) {
        ORIENTATION_LANDSCAPE -> "landscape"
        ORIENTATION_PORTRAIT -> "portrait"
        else -> null
    }

    fun addRuntimeVersionInfo(key: String, value: String) {
        runtimeVersions[key] = value
    }

    companion object {
        private val ROOT_INDICATORS = arrayOf(
            // Common binaries
            "/system/xbin/su", "/system/bin/su",
            // < Android 5.0
            "/system/app/Superuser.apk", "/system/app/SuperSU.apk",
            // >= Android 5.0
            "/system/app/Superuser", "/system/app/SuperSU",
            // Fallback
            "/system/xbin/daemonsu",
            // Systemless root
            "/su/bin"
        )
    }
}
