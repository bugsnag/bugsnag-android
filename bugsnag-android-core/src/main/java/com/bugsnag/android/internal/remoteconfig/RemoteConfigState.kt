package com.bugsnag.android.internal.remoteconfig

import com.bugsnag.android.Logger
import com.bugsnag.android.NoopLogger
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
    private val logger: Logger = runCatching { config.logger }.getOrNull() ?: NoopLogger
    private val enabled: Boolean = config.endpoints.configuration != null
    private val requestLock = Any()
    @Volatile
    private var inFlightRequest: Future<RemoteConfig?>? = null

    init {
        if (!enabled) {
            logger.d("Remote config is disabled; clearing any cached values")
            store.clear()
        }
    }

    fun scheduleDownloadIfRequired() {
        if (!enabled) {
            logger.d("Skipping remote config download because the configuration endpoint is disabled")
            return
        }

        // Check if the config is within around 2 hours of expiring
        val currentConfig = store.currentOrExpired()
        if (currentConfig != null && !shouldRefresh(currentConfig)) {
            logger.d(
                "Remote config is already available and not near expiry; skipping download " +
                    "tag=${currentConfig.configurationTag ?: "<null>"} expiry=${currentConfig.configurationExpiry.time}"
            )
            return
        }

        if (currentConfig != null) {
            logger.i(
                "Remote config is near expiry; scheduling refresh " +
                    "tag=${currentConfig.configurationTag ?: "<null>"} expiry=${currentConfig.configurationExpiry.time}"
            )
        } else {
            logger.i("No remote config available; scheduling initial download")
        }

        // Don't schedule if a request is already in-flight
        if (currentInFlightRequest() != null) {
            logger.d("Skipping remote config download because a request is already in flight")
            return
        }

        // Schedule the download in background
        try {
            logger.d("Submitting remote config download task")
            requestRemoteConfig()
        } catch (ex: Exception) {
            logger.w("Failed to schedule remote config download", ex)
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
            logger.d("Returning null remote config because the configuration endpoint is disabled")
            return null
        }

        val memoryConfig = store.current()
        if (memoryConfig != null) {
            if (shouldRefresh(memoryConfig)) {
                logger.d("In-memory remote config is near expiry; scheduling background refresh")
                scheduleDownloadIfRequired()
            }
            logger.d(
                "Returning remote config from in-memory cache " +
                    "tag=${memoryConfig.configurationTag ?: "<null>"} expiry=${memoryConfig.configurationExpiry.time}"
            )
            return memoryConfig
        }

        logger.d("No valid in-memory remote config was available; requesting with timeout=$timeout $timeUnit")

        return try {
            val remoteConfig = requestRemoteConfig().get(timeout, timeUnit)
            if (remoteConfig != null) {
                logger.i(
                    "Remote config request completed successfully " +
                        "tag=${remoteConfig.configurationTag ?: "<null>"} expiry=${remoteConfig.configurationExpiry.time}"
                )
                remoteConfig
            } else {
                logger.w("Remote config request completed without returning a config; using cached fallback if available")
                store.currentOrExpired()
            }
        } catch (ex: Exception) {
            logger.w("Failed to load remote config within the requested timeout", ex)
            store.currentOrExpired()
        }
    }

    fun getRemoteConfig(): Future<RemoteConfig?> {
        if (!enabled) {
            logger.d("Returning null future because remote config is disabled")
            return nullFuture
        }

        try {
            return requestRemoteConfig()
        } catch (ex: Exception) {
            logger.w("Failed to create remote config request future", ex)
            return nullFuture
        }
    }

    private fun requestRemoteConfig(): Future<RemoteConfig?> {
        currentInFlightRequest()?.let {
            logger.d("Reusing in-flight remote config request")
            return it
        }

        return synchronized(requestLock) {
            currentInFlightRequest()?.also {
                logger.d("Reusing in-flight remote config request after acquiring the lock")
                return@synchronized it
            }

            logger.d("Submitting remote config request task to the background executor")
            backgroundTaskService.submitTask(
                TaskType.IO,
                Callable<RemoteConfig?> {
                    try {
                        val cachedConfig = store.currentOrExpired()
                        if (cachedConfig != null && !shouldRefresh(cachedConfig)) {
                            logger.d(
                                "Loaded remote config from storage and it does not need refresh; skipping network request " +
                                    "tag=${cachedConfig.configurationTag ?: "<null>"} expiry=${cachedConfig.configurationExpiry.time}"
                            )
                            return@Callable cachedConfig
                        }

                        logger.d("Config needs refresh or is not present; requesting remote config from the endpoint")

                        return@Callable RemoteConfigRequest(
                            config,
                            notifier,
                            cachedConfig
                        ).requestConfig()?.also {
                            logger.i(
                                "Received remote config from the endpoint; persisting response " +
                                    "tag=${it.configurationTag ?: "<null>"} expiry=${it.configurationExpiry.time}"
                            )
                            store.store(it)
                        }
                    } finally {
                        clearInFlightRequest()
                    }
                }
            ).also {
                inFlightRequest = it
            }
        }
    }

    private fun currentInFlightRequest(): Future<RemoteConfig?>? = synchronized(requestLock) {
        val request = inFlightRequest
        if (request?.isDone == true) {
            logger.d("Clearing completed in-flight remote config request")
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
