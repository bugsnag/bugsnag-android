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

@Suppress("LongParameterList")
internal class RemoteConfigState(
    private val store: RemoteConfigStore,
    private val config: ImmutableConfig,
    private val notifier: Notifier,
    private val backgroundTaskService: BackgroundTaskService,
    private val clock: RemoteConfigClock = RemoteConfigClock.SYSTEM,
    /** Resolved lazily on the request thread, as loading the device ID may touch disk. */
    private val deviceIdProvider: () -> String? = { null },
    /**
     * Whether the device currently has a network connection. A fetch made while offline cannot
     * reach the config service, so it is skipped rather than counted as a failure: counting it
     * would push the backoff towards 24h and leave the config stale once the device is back online.
     */
    networkAvailable: () -> Boolean = { true },
    /** How long [connectionLost] waits before re-checking the network; 0 disables the re-check. */
    private val networkLossGraceMs: Long = NETWORK_LOSS_GRACE_MS,
) {
    /** Unknown connectivity counts as available, matching DefaultDelivery. */
    private val isNetworkAvailable: () -> Boolean = { runCatching(networkAvailable).getOrDefault(true) }

    private val logger: Logger = runCatching { config.logger }.getOrNull() ?: NoopLogger
    private val enabled: Boolean = config.endpoints.configuration != null
    private val requestLock = Any()
    @Volatile
    private var inFlightRequest: Future<RemoteConfig?>? = null

    init {
        if (!enabled) {
            logger.d("Remote config is disabled; clearing any cached values")
            store.clear()
        } else {
            scheduleSweep()
        }
    }

    /** Purges other app versions' cached config off the calling (startup) thread. */
    private fun scheduleSweep() {
        try {
            backgroundTaskService.submitTask(
                TaskType.REMOTE_CONFIG,
                Runnable {
                    try {
                        store.sweep()
                    } catch (ex: Exception) {
                        logger.w("Failed to sweep stale remote config files", ex)
                    }
                }
            )
        } catch (ex: Exception) {
            logger.w("Failed to schedule remote config sweep", ex)
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

        val skipReason = when {
            !isNetworkAvailable() -> "there is no network connection"
            currentInFlightRequest() != null -> "a request is already in flight"
            else -> null
        }
        if (skipReason != null) {
            logger.d("Skipping remote config download because $skipReason")
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
        val now = clock.currentTimeMillis()
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
            if (remoteConfig != null && !isExpired(remoteConfig)) {
                logger.i(
                    "Remote config request completed successfully " +
                        "tag=${remoteConfig.configurationTag ?: "<null>"} " +
                        "expiry=${remoteConfig.configurationExpiry.time}"
                )
                remoteConfig
            } else {
                logger.w(
                    "Remote config request completed without returning a valid config"
                )
                store.current()
            }
        } catch (ex: Exception) {
            logger.w("Failed to load remote config within the requested timeout", ex)
            store.current()
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

        return store.current()
    }

    private fun isExpired(remoteConfig: RemoteConfig): Boolean =
        remoteConfig.configurationExpiry.time <= clock.currentTimeMillis()

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
            TaskType.REMOTE_CONFIG,
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
        if (cooldownUntil > clock.currentTimeMillis()) {
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

        // the connection may have dropped since the download was scheduled
        if (!isNetworkAvailable()) {
            logger.i("Skipping remote config request because there is no network connection")
            return lockedConfig
        }

        logger.d("Config needs refresh or is not present; requesting remote config from the endpoint")
        return requestAndStoreRemoteConfig(lockedConfig)
    }

    private fun requestAndStoreRemoteConfig(lockedConfig: RemoteConfig?): RemoteConfig? {
        val deviceId = try {
            deviceIdProvider()
        } catch (ex: Exception) {
            logger.w("Failed to load device ID for remote config request", ex)
            null
        }
        val request = RemoteConfigRequest(config, notifier, lockedConfig, clock, deviceId)
        val refreshedConfig = request.requestConfig()
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

        if (connectionLost(request.failedWithoutResponse, networkLossGraceMs, isNetworkAvailable)) {
            // the connection dropped during the request: the service was never reached, so this
            // is not a failure the backoff should count
            logger.i("Remote config request failed after the network connection was lost; not starting backoff")
            return lockedConfig
        }

        // failureCount was reloaded from disk by reloadCooldownUntil() under the cross-process lock
        val failureIndex = store.failureCount()
        val cooldown = computeCooldown(failureIndex, nowMs = clock.currentTimeMillis())
        val failedCooldownUntil = cooldown.cooldownUntilMs
        store.setCooldownUntil(failedCooldownUntil, failureIndex + 1)
        logger.w(
            "Remote config request did not return a config; starting backoff " +
                "k=$failureIndex ceilingMs=${cooldown.ceilingMs} " +
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

    private fun isCooldownActive(): Boolean = store.cooldownUntil() > clock.currentTimeMillis()

    private fun isCachedCooldownActive(): Boolean {
        return store.cachedCooldownUntil() > clock.currentTimeMillis()
    }

    private fun cooldownDescription(until: Long = store.cachedCooldownUntil()): String {
        return "cooldownUntil=$until remainingMs=${(until - clock.currentTimeMillis()).coerceAtLeast(0)}"
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
        /** Long enough for Android to report a dropped network, short for a background thread. */
        const val NETWORK_LOSS_GRACE_MS = 500L

        val REFRESH_BUFFER_MS = TimeUnit.HOURS.toMillis(2)

        /** Retry delay ceiling after the first failed fetch (k = 0). */
        val BACKOFF_BASE_MS = TimeUnit.MINUTES.toMillis(15)

        /** Maximum retry delay ceiling. */
        val BACKOFF_CAP_MS = TimeUnit.HOURS.toMillis(24)

        const val BACKOFF_MULTIPLIER = 4.0

        /**
         * Computes the next allowed fetch time using exponential backoff with Equal Jitter:
         *
         * ```
         * ceiling = min(CAP, BASE * MULTIPLIER^k)
         * wait    = ceiling / 2 + random(0, ceiling / 2)
         * ```
         *
         * @param failureIndex `k`, the number of consecutive failures before this one
         * @param jitterMs fixed jitter for tests, clamped to `[0, ceiling / 2]`
         */
        internal fun computeCooldown(
            failureIndex: Int,
            jitterMs: Long? = null,
            nowMs: Long = System.currentTimeMillis()
        ): CooldownComputation {
            val ceilingMs = backoffCeiling(failureIndex)
            val halfMs = ceilingMs / 2
            val jitter = (jitterMs ?: Random.Default.nextLong(0L, halfMs + 1)).coerceIn(0L, halfMs)
            val durationMs = halfMs + jitter
            return CooldownComputation(
                ceilingMs = ceilingMs,
                durationMs = durationMs,
                cooldownUntilMs = nowMs + durationMs
            )
        }

        internal fun backoffCeiling(failureIndex: Int): Long {
            val k = failureIndex.coerceAtLeast(0)
            // pow() saturates to Infinity for large k, which min() then clamps to the cap
            val ceiling = BACKOFF_BASE_MS * Math.pow(BACKOFF_MULTIPLIER, k.toDouble())
            return minOf(BACKOFF_CAP_MS.toDouble(), ceiling).toLong()
        }

        internal data class CooldownComputation(
            val ceilingMs: Long,
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

/**
 * Whether a failed request should be put down to a lost connection rather than the config
 * service. The socket error that ends a request usually arrives a few ms *before* Android reports
 * the network as gone (20-40 ms on a Galaxy A12 when Wi-Fi dropped), so a request that failed
 * without any response, while the device still looks online, checks once more after [graceMs].
 * An HTTP error means the service was reached and is always a failure.
 */
// internal rather than private: a private top-level function called from the class needs a synthetic accessor
internal fun connectionLost(
    failedWithoutResponse: Boolean,
    graceMs: Long,
    isNetworkAvailable: () -> Boolean
): Boolean = when {
    !failedWithoutResponse -> false
    !isNetworkAvailable() -> true
    graceMs <= 0 -> false
    else -> {
        try {
            Thread.sleep(graceMs)
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        !isNetworkAvailable()
    }
}
