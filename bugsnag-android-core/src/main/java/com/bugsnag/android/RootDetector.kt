package com.bugsnag.android

import android.os.Build
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import java.io.File
import java.io.IOException
import java.io.Reader
import java.lang.Thread
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Attempts to detect whether the device is rooted. Root detection errs on the side of false
 * negatives rather than false positives.
 *
 * This class will only give a reasonable indication that a device has been rooted - as it's
 * possible to manipulate Java return values & native library loading, it will always be possible
 * for a determined application to defeat these root checks.
 */
internal class RootDetector @JvmOverloads constructor(
    private val deviceBuildInfo: DeviceBuildInfo = DeviceBuildInfo.defaultInfo(),
    private val rootBinaryLocations: List<String> = ROOT_INDICATORS,
    private val buildProps: File = BUILD_PROP_FILE,
    private val logger: Logger
) {

    companion object {
        private const val PROCESS_TIMEOUT = 250L
        private const val PROCESS_POLL_DELAY = 50L

        private val BUILD_PROP_FILE = File("/system/build.prop")

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

    @Volatile
    private var libraryLoaded = false

    init {
        try {
            System.loadLibrary("bugsnag-root-detection")
            libraryLoaded = true
        } catch (ignored: UnsatisfiedLinkError) {
            // library couldn't load. This could be due to root detection countermeasures,
            // or down to genuine OS level bugs with library loading - in either case
            // Bugsnag will default to skipping the checks.
        }
    }

    /**
     * Determines whether the device is rooted or not.
     */
    fun isRooted(): Boolean {
        return try {
            checkBuildTags() || checkSuExists() || checkBuildProps() || checkRootBinaries() || nativeCheckRoot()
        } catch (exc: Throwable) {
            logger.w("Root detection failed", exc)
            false
        }
    }

    /**
     * Checks whether the su binary exists by running `which su`. A non-empty result
     * indicates that the binary is present, which is a good indicator that the device
     * may have been rooted.
     */
    private fun checkSuExists(): Boolean = checkSuExists(ProcessBuilder())

    /**
     * Checks whether the build tags contain 'test-keys', which indicates that the OS was signed
     * with non-standard keys.
     */
    internal fun checkBuildTags(): Boolean = deviceBuildInfo.tags?.contains("test-keys") == true

    /**
     * Checks whether common root binaries exist on disk, which are a good indicator of whether
     * the device is rooted.
     */
    internal fun checkRootBinaries(): Boolean {
        runCatching {
            for (candidate in rootBinaryLocations) {
                if (File(candidate).exists()) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks the contents of /system/build.prop to see whether it contains dangerous properties.
     * These properties give a good indication that a phone might be using a custom
     * ROM and is therefore rooted.
     */
    internal fun checkBuildProps(): Boolean {
        runCatching {
            return buildProps.bufferedReader().useLines { lines ->
                lines
                    .map { line ->
                        line.replace("\\s".toRegex(), "")
                    }.filter { line ->
                        line.startsWith("ro.debuggable=[1]") || line.startsWith("ro.secure=[0]")
                    }.any()
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
            val processComplete = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(PROCESS_TIMEOUT, TimeUnit.MILLISECONDS)
            } else {
                process.fallbackWaitFor(PROCESS_TIMEOUT)
            }

            if (!processComplete) {
                return false
            }

            process.inputStream.bufferedReader().use { it.isNotBlank() }
        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt() // restore the interrupted status
            false
        } catch (ignored: IOException) {
            false
        } finally {
            process?.destroy()
        }
    }

    private external fun performNativeRootChecks(): Boolean

    private fun Reader.isNotBlank(): Boolean {
        while (true) {
            val ch = read()
            when {
                ch == -1 -> return false
                !ch.toChar().isWhitespace() -> return true
            }
        }
    }

    /**
     * Performs root checks which require native code.
     */
    private fun nativeCheckRoot(): Boolean = when {
        libraryLoaded -> performNativeRootChecks()
        else -> false
    }

    private fun Process.fallbackWaitFor(timeout: Long): Boolean {
        val endTime = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < endTime) {
            try {
                exitValue()
                return true
            } catch (ex: IllegalThreadStateException) {
                // Process is still running, wait a bit before checking again
                Thread.sleep(min(PROCESS_POLL_DELAY, endTime - SystemClock.elapsedRealtime()))
            }
        }

        return false
    }
}
