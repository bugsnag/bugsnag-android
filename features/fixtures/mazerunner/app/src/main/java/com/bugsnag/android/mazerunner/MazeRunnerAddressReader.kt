package com.bugsnag.android.mazerunner

import android.content.Context
import org.json.JSONObject
import java.io.File

const val CONFIG_FILE_TIMEOUT = 15000
const val POLL_INTERVAL_MS = 250L

object MazeRunnerAddressReader {
    fun readFromConfig(context: Context, timeout: Boolean): String? {
        val externalFilesDir = context.getExternalFilesDir(null) ?: return null
        val configFile = File(externalFilesDir, "fixture_config.json")
        CiLog.info("Attempting to read Maze Runner address from ${configFile.path}")

        if (!timeout) {
            return readFromFile(configFile)
        }

        // Poll for the fixture config file
        val pollEnd = System.currentTimeMillis() + CONFIG_FILE_TIMEOUT
        while (System.currentTimeMillis() < pollEnd) {
            val address = readFromFile(configFile)
            if (!address.isNullOrBlank()) {
                return address
            }

            Thread.sleep(POLL_INTERVAL_MS)
        }

        return null
    }

    private fun readFromFile(configFile: File): String? {
        if (!configFile.exists()) {
            return null
        }

        val fileContents = configFile.readText()
        val fixtureConfig = runCatching { JSONObject(fileContents) }.getOrNull()
        return fixtureConfig?.optString("maze_address").orEmpty().takeIf { it.isNotBlank() }
    }
}
