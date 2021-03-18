package com.bugsnag.android

import androidx.annotation.VisibleForTesting
import java.io.BufferedReader
import java.io.IOException

/**
 * Attempts to detect whether the device is rooted. Root detection errs on the side of false
 * negatives rather than false positives.
 *
 * This class will only give a reasonable indication that a device has been rooted - as it's
 * possible to manipulate Java return values & native library loading, it will always be possible
 * for a determined application to defeat these root checks.
 */
internal class RootDetector {

    fun isRooted() = checkSuExists()

    /**
     * Checks whether the su binary exists by running `which su`. A non-empty result
     * indicates that the binary is present, which is a good indicator that the device
     * may have been rooted.
     */
    fun checkSuExists(): Boolean = checkSuExists(ProcessBuilder())

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
