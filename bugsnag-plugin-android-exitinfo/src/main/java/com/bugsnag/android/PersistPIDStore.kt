package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal class PersistPIDStore(config: ImmutableConfig) {
    val file:File = File(config.persistenceDirectory.value, "bugsnag-exit-reasons.json")
    private val logger: Logger = config.logger
    private val lock = ReentrantReadWriteLock()

    fun persist(pid:Int){
        lock.writeLock().withLock {
            try {

            } catch (exc: Throwable) {
                logger.w("Unexpectedly failed to persist PID.", exc)
            }
        }
    }
}