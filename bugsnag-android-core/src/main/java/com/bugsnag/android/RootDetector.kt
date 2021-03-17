package com.bugsnag.android

import androidx.annotation.VisibleForTesting
import java.io.BufferedReader
import java.io.File
import java.io.IOException

/**
 * Attempts to detect whether the device is rooted. Root detection errs on the side of false
 * negatives rather than false positives.
 *
 * This class will only give a reasonable indication that a device has been rooted - as it's
 * possible to manipulate Java return values & native library loading, it will always be possible
 * for a determined application to defeat these root checks.
 */
internal class RootDetector(
    private val deviceBuildInfo: DeviceBuildInfo = DeviceBuildInfo.defaultInfo(),
    private val rootBinaryLocations: List<String> = ROOT_INDICATORS
) {

    companion object {
        private val ROOT_INDICATORS = listOf(
            // Common binaries
            "/system/xbin/su",
            "/system/bin/su",
            // < Android 5.0
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            // >= Android 5.0
            "/system/app/Superuser",
            "/system/app/SuperSU",
            // Fallback
            "/system/xbin/daemonsu",
            // Systemless root
            "/su/bin"
        )
    }

    /**
     * Determines whether the device is rooted or not.
     */
    fun isRooted() = checkBuildTags() || checkSuExists() || checkRootBinaries()

    /**
     * Checks whether the su binary exists by running `which su`. A non-empty result
     * indicates that the binary is present, which is a good indicator that the device
     * may have been rooted.
     */
    fun checkSuExists(): Boolean = checkSuExists(ProcessBuilder())

    /**
     * Checks whether the build tags contain 'test-keys', which indicates that the OS was signed
     * with non-standard keys.
     */
    fun checkBuildTags(): Boolean = deviceBuildInfo.tags?.contains("test-keys") == true

    /**
     * Checks whether common root binaries exist on disk, which are a good indicator of whether
     * the device is rooted.
     */
    fun checkRootBinaries(): Boolean {
        runCatching {
            for (candidate in rootBinaryLocations) {
                if (File(candidate).exists()) {
                    return true
                }
            }
        }
        return false
    }

    @VisibleForTesting
    internal fun checkSuExists(processBuilder: ProcessBuilder): Boolean {
        processBuilder.command(listOf("which", "su"))

        var process: Process? = null
        return try {
            process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
            output.isNotBlank()
        } catch (ignored: IOException) {
            false
        } finally {
            process?.destroy()
        }
    }
}
