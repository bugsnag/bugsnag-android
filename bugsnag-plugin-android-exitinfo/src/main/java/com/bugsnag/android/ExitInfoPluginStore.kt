package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import org.json.JSONObject
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal class ExitInfoPluginStore(config: ImmutableConfig) {
    private val file: File = File(config.persistenceDirectory.value, "bugsnag-exit-reasons")
    private val logger: Logger = config.logger
    private val lock = ReentrantReadWriteLock()
    private val isFirstRun: Boolean = !file.exists()

    fun persist(currentPid: Int, exitInfoKeys: Set<ExitInfoKey>) {
        lock.writeLock().withLock {
            try {
                file.writer().buffered().use { writer ->
                    JsonStream(writer).use { json ->
                        json.beginObject()
                            .name("pid").value(currentPid)
                            .name("exitInfoKeys")
                        json.value(exitInfoKeys)
                        json.endObject()
                    }
                }
            } catch (exc: Throwable) {
                logger.w("Unexpectedly failed to persist PID.", exc)
            }
        }
    }

    fun load(): Pair<Int?, Set<ExitInfoKey>> {
        if (isFirstRun) {
            return null to emptySet()
        }
        return tryLoadJson() ?: (tryLoadLegacy() to emptySet())
    }

    private fun tryLoadJson(): Pair<Int?, Set<ExitInfoKey>>? {
        try {
            val fileContents = file.readText()
            val jsonObject = JSONObject(fileContents)
            val currentPid = jsonObject.getInt("pid")
            val exitInfoKeys = mutableSetOf<ExitInfoKey>()
            val jsonArray = jsonObject.getJSONArray("exitInfoKeys")
            for (i in 0 until jsonArray.length()) {
                val exitInfoKeyObject = jsonArray.getJSONObject(i)
                exitInfoKeys.add(
                    ExitInfoKey(
                        exitInfoKeyObject.getString("pid").toInt(),
                        exitInfoKeyObject.getString("timestamp").toLong()
                    )
                )
            }
            return Pair(currentPid, exitInfoKeys)
        } catch (exc: Throwable) {
            return null
        }
    }

    private fun tryLoadLegacy(): Int? {
        try {
            val content = file.readText()
            if (content.isEmpty()) {
                logger.w("PID is empty")
                return null
            }
            return content.toIntOrNull()
        } catch (exc: Throwable) {
            logger.w("Unexpectedly failed to load PID.", exc)
            return null
        }
    }

    fun addExitInfoKey(exitInfoKey: ExitInfoKey) {
        val (oldPid, exitInfoKeys) = load()
        val newExitInfoKeys = exitInfoKeys.toMutableSet().plus(exitInfoKey)
        oldPid?.let { persist(it, newExitInfoKeys) }
    }
}
