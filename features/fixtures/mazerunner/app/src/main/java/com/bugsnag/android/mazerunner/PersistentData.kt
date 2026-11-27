package com.bugsnag.android.mazerunner

import android.content.Context
import android.os.Build
import java.io.File

class PersistentData(private val cxt: Context) {

    // Clear persistent data (used to stop scenarios bleeding into each other)
    fun clear() {
        CiLog.info("Clearing persistent data")
        clearCacheFolder("bugsnag")
        clearCacheFolder("StrictModeDiscScenarioFile")
        clearFilesFolder("background-service-dir")
        clearRemoteConfigCooldown()

        removeFile("device-id")
        removeFile("internal-device-id")

        listFolders()
    }

    // Recursively deletes the contents of a folder beneath /cache
    private fun clearCacheFolder(name: String) {
        val folder = File(cxt.cacheDir, name)
        log("Clearing folder: ${folder.path}")
        folder.deleteRecursively()
    }

    private fun clearFilesFolder(name: String) {
        val folder = File(cxt.filesDir, name)
        log("Clearing folder: ${folder.path}")
        folder.deleteRecursively()
    }

    // The SDK keeps the remote config cooldown marker outside the cache (in no_backup, or files
    // before API 21) so that it survives Android clearing the cache. Without this, a cooldown
    // started by one scenario's failed fetch would stop the next scenario from fetching.
    private fun clearRemoteConfigCooldown() {
        val baseDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cxt.noBackupFilesDir
        } else {
            cxt.filesDir
        }
        val folder = File(baseDir, "bugsnag/remote-config")
        log("Clearing folder: ${folder.path}")
        folder.deleteRecursively()
    }

    // Deletes a file beneath /files
    private fun removeFile(name: String) {
        val file = File(cxt.filesDir, name)
        log("Removing file: ${file.path}")
        file.delete()
    }

    // Logs out the contents of the /cache and /files folders
    private fun listFolders() {
        log("Contents of: ${cxt.cacheDir}")
        cxt.cacheDir.walkTopDown().forEach {
            log(it.absolutePath)
        }

        log("Contents of: ${cxt.filesDir}")
        cxt.filesDir.walkTopDown().forEach {
            log(it.absolutePath)
        }
    }
}
