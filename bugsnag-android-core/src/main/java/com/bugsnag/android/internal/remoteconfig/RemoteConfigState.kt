package com.bugsnag.android.internal.remoteconfig

import com.bugsnag.android.Notifier
import com.bugsnag.android.RemoteConfig
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.TaskType
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
    private val requestLock = Any()
    @Volatile
    private var inFlightRequest: Future<RemoteConfig?>? = null

    init {
        if (!enabled) {
            store.clear()
        }
    }


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
        if (currentInFlightRequest() != null) {
            return
        }

        // Schedule the download in background
        try {
            requestRemoteConfig()
        } catch (_: Exception) {
            clearInFlightRequest()
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
            requestRemoteConfig().get(timeout, timeUnit)
        } catch (_: Exception) {
            null
        }
    }

    fun getRemoteConfig(): Future<RemoteConfig?> {
        if (!enabled) {
            return nullFuture
        }

        try {
            return requestRemoteConfig()
        } catch (_: Exception) {
            return nullFuture
        }
    }

    private fun requestRemoteConfig(): Future<RemoteConfig?> {
        currentInFlightRequest()?.let { return it }

        return synchronized(requestLock) {
            currentInFlightRequest() ?: backgroundTaskService.submitTask(
                TaskType.IO,
                Callable<RemoteConfig?> {
                    try {
                        val remoteConfig = store.load()
                        if (remoteConfig != null) {
                            return@Callable remoteConfig
                        }

                        return@Callable RemoteConfigRequest(
                            config,
                            notifier,
                            store.currentOrExpired()
                        ).requestConfig()?.also { store.store(it) }
                    } finally {
                        clearInFlightRequest()
                    }
                }
            ).also { inFlightRequest = it }
        }
    }

    private fun currentInFlightRequest(): Future<RemoteConfig?>? = synchronized(requestLock) {
        val request = inFlightRequest
        if (request?.isDone == true) {
            inFlightRequest = null
            return null
        }

        request
    }

    private fun clearInFlightRequest() = synchronized(requestLock) {
        inFlightRequest = null
    }

    internal companion object {
        val REFRESH_BUFFER_MS = TimeUnit.HOURS.toMillis(2)

        val nullFuture = object : Future<RemoteConfig?> {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
            override fun get(): RemoteConfig? = null
            override fun get(timeout: Long, unit: TimeUnit?): RemoteConfig? = get()
            override fun isCancelled(): Boolean = false
            override fun isDone(): Boolean = true
        }
    }
}
