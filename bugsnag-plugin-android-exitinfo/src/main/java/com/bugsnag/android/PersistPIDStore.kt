package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal class PersistPIDStore(config: ImmutableConfig) {
    private val file: File = File(config.persistenceDirectory.value, "bugsnag-exit-reasons")
    private val logger: Logger = config.logger
    private val lock = ReentrantReadWriteLock()

    fun persist(pid: Int) {
        lock.writeLock().withLock {
            try {
                val text = KeyValueWriter().apply { add(pid) }.toString()
                file.writeText(text)
                logger.d("Persisted: $text")
            } catch (exc: Throwable) {
                logger.w("Unexpectedly failed to persist PID.", exc)
            }
        }
    }

    fun load(): Int? {
        return lock.readLock().withLock {
            try {
                loadImpl()
            } catch (exc: Throwable) {
                logger.w("Unexpectedly failed to load persist PID.", exc)
                null
            }
        }
    }

    private fun loadImpl(): Int? {
        return if (!file.exists()) {
            null
        } else {
            file.readText().toInt()
        }
    }
}

private class KeyValueWriter {
    private val sb = StringBuilder()
    fun add(value: Any) {
        sb.append("$value")
    }

    override fun toString() = sb.toString()
}
