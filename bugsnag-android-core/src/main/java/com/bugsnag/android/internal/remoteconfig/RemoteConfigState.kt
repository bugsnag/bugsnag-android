package com.bugsnag.android.internal.remoteconfig

import com.bugsnag.android.Notifier
import com.bugsnag.android.RemoteConfig
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.TaskType
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal class RemoteConfigState(
    private val store: RemoteConfigStore,
    private val config: ImmutableConfig,
    private val notifier: Notifier,
    private val backgroundTaskService: BackgroundTaskService,
) {
    private val enabled: Boolean = config.endpoints.configuration != null

    private val isRequestInFlight = AtomicBoolean(false)

    fun scheduleDownloadIfRequired() {
        if (!enabled) {
            return
        }

        // Check if the config is within around 2 hours of expiring
        val currentConfig = store.currentOrExpired()
        if (currentConfig != null && !shouldRefresh(currentConfig)) {
            return
        }

        // Don't schedule if a request is already in-flight
        if (!isRequestInFlight.compareAndSet(false, true)) {
            return
        }

        // Schedule the download in background
        try {
            backgroundTaskService.submitTask(TaskType.IO) {
                try {
                    val newRemoteConfig = RemoteConfigRequest(
                        config,
                        notifier,
                        store.currentOrExpired()
                    ).requestConfig()

                    // Store the new config if downloaded successfully
                    if (newRemoteConfig != null) {
                        store.store(newRemoteConfig)
                    }
                } finally {
                    isRequestInFlight.set(false)
                }
            }
        } catch (_: Exception) {
            isRequestInFlight.set(false)
        }
    }

    private fun shouldRefresh(remoteConfig: RemoteConfig): Boolean {
        val now = System.currentTimeMillis()
        val expiryTime = remoteConfig.configurationExpiry.time
        val timeUntilExpiry = expiryTime - now

        // Refresh if we're within 2 hours of expiry
        return timeUntilExpiry <= REFRESH_BUFFER_MS
    }

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
            return nullFuture
        }

        try {
            return backgroundTaskService.submitTask(
                TaskType.IO,
                Callable<RemoteConfig?> {
                    val remoteConfig = store.load()
                    if (remoteConfig != null) {
                        return@Callable remoteConfig
                    }

                    return@Callable RemoteConfigRequest(
                        config,
                        notifier,
                        store.currentOrExpired()
                    ).requestConfig()
                }
            )
        } catch (_: Exception) {
            return nullFuture
        }
    }

    internal companion object {
        const val REFRESH_BUFFER_MS = 0L

        val nullFuture = object : Future<RemoteConfig?> {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
            override fun get(): RemoteConfig? = null
            override fun get(timeout: Long, unit: TimeUnit?): RemoteConfig? = get()
            override fun isCancelled(): Boolean = false
            override fun isDone(): Boolean = true
        }
    }
}
