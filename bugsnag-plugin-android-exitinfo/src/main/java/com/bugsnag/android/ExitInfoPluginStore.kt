package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal class ExitInfoPluginStore(config: ImmutableConfig) {
    private val file: File = File(config.persistenceDirectory.value, "bugsnag-exit-reasons")
    private val logger: Logger = config.logger
    private val lock = ReentrantReadWriteLock()

    fun persist(pid: Int) {
        lock.writeLock().withLock {
            try {
                val text = pid.toString()
                file.writeText(text)
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
                logger.w("Unexpectedly failed to load PID.", exc)
                null
            }
        }
    }

    private fun loadImpl(): Int? {
        if (!file.exists()) {
            return null
        }

        val content = file.readText()
        if (content.isEmpty()) {
            logger.w("PID is empty")
            return null
        }
        return content.toIntOrNull()
    }
}
