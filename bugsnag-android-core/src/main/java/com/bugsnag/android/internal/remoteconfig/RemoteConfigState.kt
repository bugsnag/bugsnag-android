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
import kotlin.random.Random

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

        val currentConfig = store.current()
        logState("scheduleDownloadIfRequired", currentConfig)
        if (isCachedCooldownActive()) {
            logger.d("Skipping remote config download because cooldown is active ${cooldownDescription()}")
            return
        }
        // Check if the config is within around 2 hours of expiring
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

        logState("getRemoteConfig cache miss")
        logger.d("No valid in-memory remote config was available; requesting with timeout=$timeout $timeUnit")

        return try {
            val remoteConfig = requestRemoteConfig().get(timeout, timeUnit)
            if (remoteConfig != null) {
                logger.i(
                    "Remote config request completed successfully " +
                        "tag=${remoteConfig.configurationTag ?: "<null>"} " +
                        "expiry=${remoteConfig.configurationExpiry.time}"
                )
                remoteConfig
            } else {
                logger.w(
                    "Remote config request completed without returning a config; " +
                        "using cached fallback if available"
                )
                store.cachedCurrentOrExpired()
            }
        } catch (ex: Exception) {
            logger.w("Failed to load remote config within the requested timeout", ex)
            store.cachedCurrentOrExpired()
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

    fun peekRemoteConfig(): RemoteConfig? {
        if (!enabled) {
            return null
        }

        return store.currentOrExpired()
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

            val currentConfig = store.current()
            if (currentConfig != null && !shouldRefresh(currentConfig)) {
                logger.d("Returning fresh in-memory remote config without scheduling a request")
                return@synchronized immediateFuture(currentConfig)
            }
            logger.d("Submitting remote config request task to the background executor")
            submitRemoteConfigRequest().also {
                inFlightRequest = it
            }
        }
    }

    private fun submitRemoteConfigRequest(): Future<RemoteConfig?> {
        return backgroundTaskService.submitTask(
            TaskType.IO,
            Callable<RemoteConfig?> {
                try {
                    loadOrRequestRemoteConfig()
                } finally {
                    clearInFlightRequest()
                }
            }
        )
    }

    private fun loadOrRequestRemoteConfig(): RemoteConfig? {
        val cachedConfig = store.currentOrExpired()
        logState("requestRemoteConfig", cachedConfig)
        if (isCooldownActive()) {
            logger.i("Skipping remote config request because cooldown is active ${cooldownDescription()}")
            return cachedConfig
        }

        return store.withCrossProcessLock {
            requestRemoteConfigWithLock()
        } ?: cachedConfig
    }

    private fun requestRemoteConfigWithLock(): RemoteConfig? {
        val lockedConfig = store.reloadCurrentOrExpired()
        val cooldownUntil = store.reloadCooldownUntil()
        if (cooldownUntil > System.currentTimeMillis()) {
            logger.i(
                "Skipping remote config request after acquiring cross-process lock because " +
                    "cooldown is active ${cooldownDescription()}"
            )
            return lockedConfig
        }

        if (lockedConfig != null && !shouldRefresh(lockedConfig)) {
            logger.d(
                "Reloaded remote config from storage after acquiring cross-process lock and it does not " +
                    "need refresh; skipping network request ${describeRemoteConfig(lockedConfig)}"
            )
            return lockedConfig
        }

        logger.d("Config needs refresh or is not present; requesting remote config from the endpoint")
        return requestAndStoreRemoteConfig(lockedConfig)
    }

    private fun requestAndStoreRemoteConfig(lockedConfig: RemoteConfig?): RemoteConfig? {
        val refreshedConfig = RemoteConfigRequest(config, notifier, lockedConfig).requestConfig()
        if (refreshedConfig != null) {
            logger.i(
                "Received remote config from the endpoint; persisting response " +
                    describeRemoteConfig(refreshedConfig)
            )
            store.store(refreshedConfig)
            store.clearCooldown()
            logState("remote config stored", refreshedConfig)
            return refreshedConfig
        }

        val cooldown = computeCooldown(RETRY_COOLDOWN_MS)
        val failedCooldownUntil = cooldown.cooldownUntilMs
        store.setCooldownUntil(failedCooldownUntil)
        logger.w(
            "Remote config request did not return a config; starting cooldown " +
                "baseMs=$RETRY_COOLDOWN_MS jitterMs=${cooldown.jitterMs} " +
                "durationMs=${cooldown.durationMs} until=$failedCooldownUntil"
        )
        logState("remote config request failed", lockedConfig)
        return lockedConfig
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

    private fun isCooldownActive(): Boolean = store.cooldownUntil() > System.currentTimeMillis()

    private fun isCachedCooldownActive(): Boolean {
        return store.cachedCooldownUntil() > System.currentTimeMillis()
    }

    private fun cooldownDescription(until: Long = store.cachedCooldownUntil()): String {
        return "cooldownUntil=$until remainingMs=${(until - System.currentTimeMillis()).coerceAtLeast(0)}"
    }

    private fun logState(marker: String, remoteConfig: RemoteConfig? = null) {
        logger.d(
            "Remote config state marker=$marker inFlight=${currentInFlightRequest() != null} " +
                "config=${remoteConfig?.let(::describeRemoteConfig) ?: "<none>"} " +
                cooldownDescription()
        )
    }

    private fun describeRemoteConfig(remoteConfig: RemoteConfig): String {
        return "tag=${remoteConfig.configurationTag ?: "<null>"}, " +
            "expiry=${remoteConfig.configurationExpiry.time}, " +
            "discardRules=${remoteConfig.discardRules.size}"
    }

    private fun immediateFuture(remoteConfig: RemoteConfig?) = object : Future<RemoteConfig?> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        override fun get(): RemoteConfig? = remoteConfig
        override fun get(timeout: Long, unit: TimeUnit?): RemoteConfig? = remoteConfig
        override fun isCancelled(): Boolean = false
        override fun isDone(): Boolean = true
    }

    internal companion object {
        val REFRESH_BUFFER_MS = TimeUnit.HOURS.toMillis(2)
        val RETRY_COOLDOWN_MS = TimeUnit.HOURS.toMillis(24)
        val COOLDOWN_JITTER_MS = TimeUnit.HOURS.toMillis(2)

        internal fun computeCooldown(
            baseCooldownMs: Long,
            jitterOffsetMs: Long? = null,
            jitterRangeMs: Long = COOLDOWN_JITTER_MS,
            nowMs: Long = System.currentTimeMillis()
        ): CooldownComputation {
            val maxJitter = jitterRangeMs.coerceAtLeast(0L)
            val jitterMs = jitterOffsetMs ?: Random.Default.nextLong(-maxJitter, maxJitter + 1)
            val durationMs = (baseCooldownMs + jitterMs).coerceAtLeast(0L)
            return CooldownComputation(
                jitterMs = jitterMs,
                durationMs = durationMs,
                cooldownUntilMs = nowMs + durationMs
            )
        }

        internal data class CooldownComputation(
            val jitterMs: Long,
            val durationMs: Long,
            val cooldownUntilMs: Long
        )

        val nullFuture = object : Future<RemoteConfig?> {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
            override fun get(): RemoteConfig? = null
            override fun get(timeout: Long, unit: TimeUnit?): RemoteConfig? = get()
            override fun isCancelled(): Boolean = false
            override fun isDone(): Boolean = true
        }
    }
}
