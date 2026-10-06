package com.bugsnag.android

import android.app.ApplicationExitInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.bugsnag.android.internal.ImmutableConfig
import org.json.JSONObject
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * A store for persisting the state of the exit info plugin across application restarts. We track
 * two internal states:
 * 1. `previousState`: The state from the last time the app was run
 * 2. `currentState`: The state for the current run of the app, which is updated as we run
 *
 * The rules for processing an `ApplicationExitInfo` are as follows:
 * - it is only processed if it was not already processed on a previous run (controlled by timestamp)
 * - it is only processed once per run of the app (controlled by `currentState.processedExitInfoKeys`)
 * - we do not assume that the exit info will be processed in any order
 * - we do not process any exit info if there is no previous state (either because this is the first
 *   run of the app or the store was deleted, or because the store was corrupted)
 */
@RequiresApi(Build.VERSION_CODES.R)
internal class ExitInfoPluginStore(
    private val file: File,
    private val logger: Logger
) {
    private val lock = ReentrantReadWriteLock()

    constructor(config: ImmutableConfig) : this(
        File(config.persistenceDirectory.value, "bugsnag-exit-reasons"),
        config.logger,
    )

    var previousState: PersistentState? = null
        @VisibleForTesting
        internal set

    var currentState: PersistentState = PersistentState(
        android.os.Process.myPid(), System.currentTimeMillis(), HashSet()
    )
        @VisibleForTesting
        internal set

    init {
        load()
    }

    private fun load() {
        lock.readLock().withLock {
            previousState = tryLoadJson()
            if (previousState == null) {
                previousState = tryLoadLegacy()
            }

            val timestamp = maxOf(System.currentTimeMillis(), previousState?.newestTimestamp ?: 0L)
            currentState = PersistentState(
                pid = android.os.Process.myPid(),
                timestamp = timestamp,
                // we never carry over any exit info keys since the "new" state timestamp
                // will always be greater than any previous state exit info key timestamp
                processedExitInfoKeys = emptySet()
            )
        }
    }

    private fun persist() {
        lock.writeLock().withLock {
            try {
                file.writer().buffered().use { writer ->
                    JsonStream(writer).use { json ->
                        currentState.toStream(json)
                    }
                }
            } catch (exc: Throwable) {
                tryDeleteStore()
                logger.d(
                    "Unexpectedly failed to persist PID, historical exit reasons may " +
                        "not be matchable if this process crashes.",
                    exc
                )
            }
        }
    }

    private fun tryDeleteStore() {
        try {
            // attempt to delete the file if it exists, this is done in extreme cases
            // in an attempt to cause the next startup to reinitialize the store as "new"
            file.delete()
        } catch (_: Throwable) {
        }
    }

    private fun tryLoadJson(): PersistentState? {
        try {
            val fileContents = file.readText()
            val jsonObject = JSONObject(fileContents)
            return parsePersistentState(jsonObject)
        } catch (exc: Throwable) {
            return null
        }
    }

    private fun tryLoadLegacy(): PersistentState? {
        try {
            val content = file.readText()
            if (content.isEmpty()) {
                return null
            }
            val previousPid = content.toIntOrNull()
            if (previousPid != null) {
                return PersistentState(previousPid, System.currentTimeMillis(), emptySet())
            }
        } catch (_: Throwable) {
        }

        return null
    }

    fun addExitInfoKeys(exitInfo: Iterable<ExitInfoKey>) {
        currentState += exitInfo
        persist()
    }

    fun addExitInfoKey(exitInfoKey: ExitInfoKey) {
        currentState += exitInfoKey
        persist()
    }

    private fun parsePersistentState(json: JSONObject): PersistentState {
        val pid = json.getInt("pid")
        val timestamp = json.optString("timestamp").toLongOrNull() ?: System.currentTimeMillis()
        val keysArray = json.getJSONArray("exitInfoKeys")
        val exitInfoKeys = HashSet<ExitInfoKey>(keysArray.length())
        for (i in 0 until keysArray.length()) {
            val keyObject = keysArray.getJSONObject(i)

            exitInfoKeys.add(
                ExitInfoKey(
                    keyObject.getString("pid").toInt(),
                    keyObject.getString("timestamp").toLong()
                )
            )
        }
        return PersistentState(pid, timestamp, exitInfoKeys)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    internal data class PersistentState(
        /**
         * The PID of the process that created and stored this `PersistentState`
         */
        val pid: Int,
        /**
         * The timestamp when this `PersistentState` was created, when scanning `ApplicationExitInfo`
         * only exit reasons with a timestamp greater than this will be considered.
         */
        val timestamp: Long,
        /**
         * A set of exit info keys that have already been processed. This is used to avoid
         * processing the same exit reason multiple times.
         */
        val processedExitInfoKeys: Set<ExitInfoKey>,
    ) : JsonStream.Streamable {

        val newestTimestamp: Long
            get() = processedExitInfoKeys.maxOfOrNull { it.timestamp } ?: timestamp

        operator fun plus(exitInfo: ExitInfoKey): PersistentState {
            return PersistentState(pid, timestamp, processedExitInfoKeys + exitInfo)
        }

        operator fun plus(exitInfo: Iterable<ExitInfoKey>): PersistentState {
            return PersistentState(pid, timestamp, processedExitInfoKeys + exitInfo)
        }

        fun filterApplicationExitInfo(
            exitInfo: Iterable<ApplicationExitInfo>,
        ): List<ApplicationExitInfo> {
            // Only include exit reasons that are newer than the timestamp and not already processed
            return exitInfo.filter {
                it.timestamp > timestamp && !processedExitInfoKeys.contains(ExitInfoKey(it))
            }
        }

        override fun toStream(writer: JsonStream) {
            writer.beginObject()
                .name("pid").value(pid)
                .name("timestamp").value(timestamp.toString())
                .name("exitInfoKeys").value(processedExitInfoKeys)
            writer.endObject()
        }
    }
}
