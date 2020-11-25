package com.bugsnag.android

import android.util.JsonReader
import java.io.File
import java.io.IOException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal class SynchronizedStreamableStore<T : JsonStream.Streamable>(
    private val file: File
) {

    private val lock = ReentrantReadWriteLock()

    @Throws(IOException::class)
    fun persist(streamable: T) {
        lock.writeLock().withLock {
            file.writer().use {
                streamable.toStream(JsonStream(it))
                true
            }
        }
    }

    @Throws(IOException::class)
    fun load(loadCallback: (JsonReader) -> T): T {
        lock.readLock().withLock {
            return file.reader().use {
                loadCallback(JsonReader(it))
            }
        }
    }
}
