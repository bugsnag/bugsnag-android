package com.bugsnag.android.internal

import com.bugsnag.android.Notifier
import com.bugsnag.android.RemoteConfig
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal class RemoteConfigState(
    private val store: RemoteConfigStore,
    private val config: ImmutableConfig,
    private val notifier: Notifier,
    private val backgroundTaskService: BackgroundTaskService,
) {
    private val enabled: Boolean = config.endpoints.configuration != null

    fun getRemoteConfig(timeout: Long, timeUnit: TimeUnit): RemoteConfig? {
        if (!enabled) {
            return null
        }

        val memoryConfig = store.current()
        if (memoryConfig != null) {
            return memoryConfig
        }

        return try {
            getRemoteConfig().get(timeout, timeUnit)
        } catch (_: Exception) {
            null
        }
    }

    fun getRemoteConfig(): Future<RemoteConfig?> {
        if (!enabled) {
            return object : Future<RemoteConfig?> {
                override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
                override fun get(): RemoteConfig? = null
                override fun get(timeout: Long, unit: TimeUnit?): RemoteConfig? = get()
                override fun isCancelled(): Boolean = false
                override fun isDone(): Boolean = true
            }
        }

        return backgroundTaskService.submitTask(TaskType.IO, Callable<RemoteConfig?> {
            val remoteConfig = store.load()
            if (remoteConfig != null) {
                return@Callable remoteConfig
            }

            return@Callable RemoteConfigRequest(
                config,
                notifier,
                store.currentOrExpired()
            ).call()
        })
    }
}
