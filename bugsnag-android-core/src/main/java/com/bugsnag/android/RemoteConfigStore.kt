package com.bugsnag.android

import android.util.JsonReader
import java.io.File
import java.io.FileReader
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal class RemoteConfigStore(
    private val persistentDir: File,
    private val appVersionCode: Int
) {
    val file: File =
        File(persistentDir, "core-$appVersionCode.json")
    private val lock = ReentrantReadWriteLock()

    init {
        load()
    }

    fun load(): RemoteConfig? {
        return lock.readLock().withLock {
            if (file.exists()) {
                RemoteConfig.fromReader(JsonReader(FileReader(file)))
            } else {
                null
            }
        }
    }

    fun update(remoteConfig: RemoteConfig?) {
        lock.writeLock().withLock {
            if (remoteConfig == null) {
                if (file.exists()) {
                    file.delete()
                }
            } else {
                file.parentFile?.mkdirs()
                file.outputStream().bufferedWriter().use { writer ->
                    JsonStream(writer).use { stream ->
                        remoteConfig.toStream(stream)
                    }
                }
            }
        }
    }
}
