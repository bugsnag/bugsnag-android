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
    internal val isFirstRun: Boolean = !file.exists()

    internal var legacyStore: Boolean = false
        private set

    var previousPid: Int = 0
        private set

    var currentPid: Int = 0

    private var _exitInfoKeys = HashSet<ExitInfoKey>()
    val exitInfoKeys: Set<ExitInfoKey> get() = _exitInfoKeys

    init {
        load()
    }

    private fun load() {
        lock.readLock().withLock {
            val json = tryLoadJson()
            if (json != null) {
                if (previousPid == 0) {
                    previousPid = json.first ?: 0
                } else {
                    currentPid = json.first ?: 0
                }

                _exitInfoKeys = json.second
            } else {
                val legacy = tryLoadLegacy()
                if (legacy != null) {
                    previousPid = legacy
                }
            }
        }
    }

    private fun persist() {
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

    private fun tryLoadJson(): Pair<Int?, HashSet<ExitInfoKey>>? {
        try {
            val fileContents = file.readText()
            val jsonObject = JSONObject(fileContents)
            val currentPid = jsonObject.getInt("pid")
            val exitInfoKeys = HashSet<ExitInfoKey>()
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
        _exitInfoKeys.add(exitInfoKey)
        persist()
    }
}
