package com.bugsnag.android

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.SystemClock

import java.util.HashMap

/**
 * Collects various data on the application state
 */
internal class AppDataCollector(
    appContext: Context,
    private val packageManager: PackageManager?,
    private val config: ImmutableConfig,
    private val sessionTracker: SessionTracker,
    private val activityManager: ActivityManager?,
    private val logger: Logger
) {

    private val packageName: String = appContext.packageName
    private var packageInfo = packageManager?.getPackageInfo(packageName, 0)
    private var appInfo: ApplicationInfo? = packageManager?.getApplicationInfo(packageName, 0)

    private var binaryArch: String? = null
    private val appName = getAppName()
    private val releaseStage = guessReleaseStage()
    private val versionName = config.appVersion ?: packageInfo?.versionName

    fun generateApp(): App = App(config, binaryArch, packageName, releaseStage, versionName)

    fun generateAppWithState(): AppWithState = AppWithState(
        config, binaryArch, packageName, releaseStage, versionName,
        getDurationMs(), calculateDurationInForeground(), sessionTracker.isInForeground
    )

    fun getAppDataMetadata(): MutableMap<String, Any?> {
        val map = HashMap<String, Any?>()
        map["name"] = appName
        map["activeScreen"] = getActiveScreenClass()
        map["memoryUsage"] = getMemoryUsage()
        map["lowMemory"] = isLowMemory()
        return map
    }

    fun getActiveScreenClass(): String? = sessionTracker.contextActivity

    /**
     * Get the actual memory used by the VM (which may not be the total used
     * by the app in the case of NDK usage).
     */
    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    /**
     * Check if the device is currently running low on memory.
     */
    private fun isLowMemory(): Boolean? {
        try {
            if (activityManager != null) {
                val memInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memInfo)
                return memInfo.lowMemory
            }
        } catch (exception: Exception) {
            logger.w("Could not check lowMemory status")
        }
        return null
    }

    fun setBinaryArch(binaryArch: String) {
        this.binaryArch = binaryArch
    }

    /**
     * Calculates the duration the app has been in the foreground
     *
     * @return the duration in ms
     */
    internal fun calculateDurationInForeground(): Long {
        val nowMs = System.currentTimeMillis()
        return sessionTracker.getDurationInForegroundMs(nowMs)
    }

    /**
     * Guess the release stage of the running Android app by checking the
     * android:debuggable flag from AndroidManifest.xml. If the release stage was set in
     * [Configuration], this value will be returned instead.
     */
    private fun guessReleaseStage(): String {
        return when {
            config.releaseStage != null -> config.releaseStage
            appInfo != null && (appInfo!!.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) -> {
                RELEASE_STAGE_DEVELOPMENT
            }
            else -> RELEASE_STAGE_PRODUCTION
        }
    }

    /**
     * The name of the running Android app, from android:label in
     * AndroidManifest.xml
     */
    private fun getAppName(): String? {
        val hasInfo = packageManager != null && appInfo != null
        return when {
            hasInfo -> packageManager?.getApplicationLabel(appInfo).toString()
            else -> null
        }
    }

    companion object {

        internal val startTimeMs = SystemClock.elapsedRealtime()

        private const val RELEASE_STAGE_DEVELOPMENT = "development"
        const val RELEASE_STAGE_PRODUCTION = "production"

        /**
         * Get the time in milliseconds since Bugsnag was initialized, which is a
         * good approximation for how long the app has been running.
         */
        fun getDurationMs(): Long = SystemClock.elapsedRealtime() - startTimeMs
    }

}
