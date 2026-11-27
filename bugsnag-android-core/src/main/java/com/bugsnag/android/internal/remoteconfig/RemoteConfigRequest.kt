package com.bugsnag.android.internal.remoteconfig

import android.os.Build
import androidx.annotation.VisibleForTesting
import com.bugsnag.android.Logger
import com.bugsnag.android.Notifier
import com.bugsnag.android.RemoteConfig
import com.bugsnag.android.internal.HEADER_BUGSNAG_API_KEY
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.JsonCollectionParser
import com.bugsnag.android.internal.JsonCollectionParser.JsonParseException
import com.bugsnag.android.internal.JsonHelper
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Suppress("LongParameterList")
internal class RemoteConfigRequest(
    private val baseUrl: String?,
    private val apiKey: String,
    private val notifier: Notifier,
    private val appVersion: String?,
    private val versionCode: Int?,
    private val releaseStage: String?,
    private val packageName: String?,
    private val remoteConfig: RemoteConfig?,
    private val logger: Logger,
    private val clock: RemoteConfigClock = RemoteConfigClock.SYSTEM,
    private val deviceId: String? = null,
    private val requestDeadlineMs: Long = REQUEST_DEADLINE_MS
) {
    constructor(
        config: ImmutableConfig,
        notifier: Notifier,
        currentRemoteConfig: RemoteConfig?,
        clock: RemoteConfigClock = RemoteConfigClock.SYSTEM,
        deviceId: String? = null
    ) : this(
        config.endpoints.configuration,
        config.apiKey,
        notifier,
        config.appVersion ?: config.packageInfo?.versionName,
        config.versionCode,
        config.releaseStage,
        config.packageInfo?.packageName,
        currentRemoteConfig,
        config.logger,
        clock,
        deviceId
    )

    /**
     * True when the last [requestConfig] failed before the config service sent any response: the
     * connection, DNS lookup or socket failed, or the request hit its deadline. Unlike an HTTP
     * error, such a failure may be caused by the device losing its network mid-request.
     */
    @Volatile
    var failedWithoutResponse: Boolean = false
        private set

    fun requestConfig(): RemoteConfig? {
        failedWithoutResponse = false
        if (baseUrl == null) {
            logger.d("Skipping remote config request because the configuration endpoint is not configured")
            return null
        }

        val startedAtNanos = System.nanoTime()
        return try {
            requestNewConfig(startedAtNanos)
        } catch (ex: Exception) {
            failedWithoutResponse = isNoResponseFailure(ex)
            // The cause is spelled out in the message as well: Android logs an empty stack trace
            // for UnknownHostException, so the throwable alone does not show why a request failed.
            logger.w(
                "Remote config request failed after ${elapsedMs(startedAtNanos)}ms " +
                    "(${ex.javaClass.name}: ${ex.message}); returning null",
                ex
            )
            null
        }
    }

    /** Connection, DNS and socket errors, and deadline timeouts, all happen before any response. */
    private fun isNoResponseFailure(ex: Exception): Boolean =
        ex is IOException || ex is RemoteConfigTimeoutException

    private fun requestNewConfig(startedAtNanos: Long): RemoteConfig? {
        val urlWithParams = buildUrlWithQueryParameters() ?: return null

        logger.i(
            "Requesting remote config from $urlWithParams " +
                "cachedTag=${remoteConfig?.configurationTag ?: "<none>"}"
        )

        val url = URL(urlWithParams)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.doOutput = false
        connection.connectTimeout = REQUEST_TIMEOUT_MS
        connection.readTimeout = REQUEST_TIMEOUT_MS

        // Set required headers
        connection.setRequestProperty(HEADER_BUGSNAG_API_KEY, apiKey)
        connection.setRequestProperty(HEADER_BUGSNAG_NOTIFIER_NAME, notifier.name)
        connection.setRequestProperty(HEADER_BUGSNAG_NOTIFIER_VERSION, notifier.version)
        // Sent on any scheme (like device.id in event payloads) so that on-premise installs
        // using http:// config endpoints behave the same as https:// ones.
        if (!deviceId.isNullOrEmpty()) {
            connection.setRequestProperty(HEADER_BUGSNAG_DEVICE_ID, deviceId)
        }

        if (remoteConfig?.configurationTag != null) {
            connection.setRequestProperty(HEADER_IF_NONE_MATCH, remoteConfig.configurationTag)
            logger.d("Sending remote config request with If-None-Match=${remoteConfig.configurationTag}")
        } else {
            logger.d("Sending remote config request without If-None-Match header")
        }
        logger.d(
            "Remote config request headers: $HEADER_BUGSNAG_API_KEY=${redactApiKey(apiKey)} " +
                "$HEADER_BUGSNAG_NOTIFIER_NAME=${notifier.name} " +
                "$HEADER_BUGSNAG_NOTIFIER_VERSION=${notifier.version} " +
                "$HEADER_BUGSNAG_DEVICE_ID=${if (deviceId.isNullOrEmpty()) "<absent>" else "<present>"} " +
                "$HEADER_IF_NONE_MATCH=${remoteConfig?.configurationTag ?: "<absent>"}"
        )

        return exchangeWithDeadline(connection, startedAtNanos)
    }

    /**
     * Runs the blocking part of the request (DNS lookup, connect, TLS handshake, response) on
     * [requestExecutor] and waits at most [requestDeadlineMs] for it.
     *
     * connect/read timeouts do not cover DNS resolution, and are not always honoured during the
     * TLS handshake, and [HttpURLConnection.disconnect] cannot interrupt a blocked DNS lookup
     * either. Waiting on a separate thread is the only way to bound the time this caller (which
     * holds the in-flight request and the cross-process lock) is blocked. A worker that is stuck
     * past the deadline is abandoned: it finishes when the OS gives up and its result is dropped.
     */
    private fun exchangeWithDeadline(connection: HttpURLConnection, startedAtNanos: Long): RemoteConfig? {
        val exchange = submitExchange(connection, startedAtNanos)
        try {
            val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(requestDeadlineMs)
            return awaitIgnoringInterrupts(exchange, deadlineNanos)
        } catch (exc: TimeoutException) {
            logger.w("Remote config request exceeded ${requestDeadlineMs}ms; aborting")
            abandon(exchange, connection)
            throw RemoteConfigTimeoutException("Remote config request exceeded ${requestDeadlineMs}ms", exc)
        } catch (exc: ExecutionException) {
            // surface the real failure (e.g. UnknownHostException) rather than the wrapper
            throw (exc.cause as? Exception) ?: exc
        }
    }

    /**
     * Waits for [exchange] until [deadlineNanos], carrying on through interrupts and restoring the
     * interrupt flag afterwards.
     *
     * An interrupt is not a failed request: the SDK's executors are interrupted when the app is
     * crashing or shutting down, often just before the response arrives. Treating that as a failure
     * would drop a good response and start a backoff cooldown. Blocking network I/O, which this
     * request ran on the calling thread before, is not interruptible either.
     */
    private fun awaitIgnoringInterrupts(exchange: Future<RemoteConfig?>, deadlineNanos: Long): RemoteConfig? {
        var interrupted = false
        try {
            while (true) {
                try {
                    // a timeout that has already passed makes get() throw TimeoutException
                    return exchange.get(deadlineNanos - System.nanoTime(), TimeUnit.NANOSECONDS)
                } catch (exc: InterruptedException) {
                    interrupted = true
                }
            }
        } finally {
            if (interrupted) {
                logger.d("Remote config request wait was interrupted; kept waiting for the response")
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun submitExchange(connection: HttpURLConnection, startedAtNanos: Long): Future<RemoteConfig?> {
        try {
            return requestExecutor.submit(Callable { exchange(connection, startedAtNanos) })
        } catch (exc: RejectedExecutionException) {
            // every worker is still stuck on an earlier request that already passed its deadline
            connection.disconnect()
            throw RemoteConfigTimeoutException(
                "Remote config request not sent: $MAX_REQUEST_THREADS earlier request(s) are still blocked",
                exc
            )
        }
    }

    private fun abandon(exchange: Future<RemoteConfig?>, connection: HttpURLConnection) {
        exchange.cancel(true)
        try {
            // unblocks a stalled socket read or TLS handshake; a DNS lookup keeps running
            connection.disconnect()
        } catch (ignored: Exception) {
        }
    }

    private fun exchange(connection: HttpURLConnection, startedAtNanos: Long): RemoteConfig? {
        try {
            val responseCode = connection.responseCode
            logger.i(
                "Remote config response received code=$responseCode message=${connection.responseMessage} " +
                    "etag=${connection.getHeaderField(HEADER_ETAG)} " +
                    "cacheControl=${connection.getHeaderField(HEADER_CACHE_CONTROL)} " +
                    "contentLength=${connection.contentLength} " +
                    "elapsedMs=${elapsedMs(startedAtNanos)}"
            )
            return when (responseCode) {
                HttpURLConnection.HTTP_OK -> parseRemoteConfig(connection)
                HttpURLConnection.HTTP_NOT_MODIFIED -> {
                    val expiryDate = configExpiryDate(connection)
                    logger.i("Remote config not modified; refreshing cached config expiry to ${expiryDate.time}")
                    renewExistingConfig(expiryDate, connection.getHeaderField(HEADER_ETAG))
                }

                HttpURLConnection.HTTP_BAD_REQUEST -> {
                    logger.w("Remote config request returned HTTP 400; returning null to start cooldown")
                    null
                }

                else -> {
                    logger.w("Remote config request returned unexpected response code $responseCode")
                    null
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun elapsedMs(startedAtNanos: Long): Long =
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos)

    /** Only the last few characters, enough to tell which project's key was sent. */
    private fun redactApiKey(key: String): String =
        if (key.length <= API_KEY_VISIBLE_CHARS) "****" else "****" + key.takeLast(API_KEY_VISIBLE_CHARS)

    private fun buildUrlWithQueryParameters(): String? = baseUrl?.let { urlBase ->
        buildString {
            append(urlBase)
            if (last() != '/') {
                append('/')
            }
            append("error-config")

            // Add osVersion (required)
            append("?osVersion=")
            append(Build.VERSION.SDK_INT)

            // Add optional parameters
            appVersion?.let { version ->
                append("&version=")
                append(urlEncoded(version))
            }

            versionCode?.let { versionCode ->
                append("&versionCode=")
                append(versionCode)
            }

            releaseStage?.let { releaseStage ->
                append("&releaseStage=")
                append(urlEncoded(releaseStage))
            }

            packageName?.let { appId ->
                append("&appId=")
                append(urlEncoded(appId))
            }
        }
    }

    @VisibleForTesting
    internal fun parseRemoteConfig(connection: HttpURLConnection): RemoteConfig? {
        val expiryDate = configExpiryDate(connection)
        val tag = connection.getHeaderField(HEADER_ETAG)

        // we may receive an empty response as a valid "no specific config" value
        if (connection.contentLength == 0) {
            logger.i(
                "Remote config response body was empty; returning empty config " +
                    "tag=${tag ?: "<null>"} expiry=${expiryDate.time}"
            )
            return RemoteConfig(tag, expiryDate, emptyList())
        }

        val json = try {
            connection.inputStream.use { inputStream ->
                val parser = JsonCollectionParser(inputStream)

                @Suppress("UNCHECKED_CAST")
                parser.parse() as? LinkedHashMap<String, Any?>
            }
        } catch (_: JsonParseException) {
            // these can happen when the response is empty, but the Content-Length was not set
            logger.i(
                "Remote config response body was empty or malformed; returning empty config " +
                    "tag=${tag ?: "<null>"} expiry=${expiryDate.time}"
            )
            return RemoteConfig(tag, expiryDate, emptyList())
        }

        if (json == null) {
            return null
        }

        val remoteConfig = RemoteConfig.fromJsonMap(tag, expiryDate, json)
        logger.d("Fetched RemoteConfig JSON: ${String(JsonHelper.serialize(remoteConfig), Charsets.UTF_8)}")
        logger.i(
            "Parsed remote config response tag=${tag ?: "<null>"} " +
                "discardRules=${remoteConfig.discardRules.size} expiry=${expiryDate.time}"
        )
        return remoteConfig
    }

    @Suppress("ReturnCount")
    private fun configExpiryDate(connection: HttpURLConnection): Date {

        val cacheControl = connection.getHeaderField(HEADER_CACHE_CONTROL)
            ?: return defaultConfigExpiry("missing Cache-Control header")

        val maxAgeMatcher = maxAgeRegex.matchEntire(cacheControl)
            ?: return defaultConfigExpiry("unrecognized Cache-Control header '$cacheControl'")
        val maxAgeSeconds = maxAgeMatcher.groupValues.getOrNull(1)?.toLongOrNull()
            ?: return defaultConfigExpiry("invalid max-age value in Cache-Control header '$cacheControl'")

        if (maxAgeSeconds < 0L) {
            return defaultConfigExpiry("negative max-age value in Cache-Control header '$cacheControl'")
        }

        val now = clock.currentTimeMillis()
        val maxDuration = Long.MAX_VALUE - now
        val duration = if (maxAgeSeconds > maxDuration / SECONDS_MS) {
            logger.w("Remote config max-age is too large; clamping expiry to the maximum supported timestamp")
            maxDuration
        } else {
            maxAgeSeconds * SECONDS_MS
        }
        val expiry = Date(now + duration)
        logger.d("Remote config expiry parsed from Cache-Control='$cacheControl' -> ${expiry.time}")
        return expiry
    }

    private fun urlEncoded(value: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            URLEncoder.encode(value, StandardCharsets.UTF_8)
        } else {
            URLEncoder.encode(value, "UTF-8")
        }
    }

    private fun renewExistingConfig(configExpiryDate: Date, responseTag: String?): RemoteConfig? {
        if (remoteConfig == null) {
            logger.w("Received HTTP 304 but there is no cached remote config to renew")
            return null
        }

        if (responseTag != null && responseTag != remoteConfig.configurationTag) {
            logger.w(
                "Received HTTP 304 with a different ETag; refusing to renew cached remote config " +
                    "cachedTag=${remoteConfig.configurationTag ?: "<null>"} responseTag=$responseTag"
            )
            return null
        }

        logger.d(
            "Renewing cached remote config tag=${remoteConfig.configurationTag ?: "<null>"} " +
                "with new expiry=${configExpiryDate.time}"
        )
        return RemoteConfig(
            remoteConfig.configurationTag,
            configExpiryDate,
            remoteConfig.discardRules
        )
    }

    private fun defaultConfigExpiry(reason: String): Date {
        val expiry = Date(clock.currentTimeMillis() + DEFAULT_CONFIG_EXPIRY_TIME)
        logger.d("Using default remote config expiry because $reason -> ${expiry.time}")
        return expiry
    }

    internal companion object {
        const val HEADER_ETAG = "ETag"
        const val HEADER_CACHE_CONTROL = "Cache-Control"
        const val HEADER_IF_NONE_MATCH = "If-None-Match"
        const val HEADER_BUGSNAG_NOTIFIER_NAME = "Bugsnag-Notifier-Name"
        const val HEADER_BUGSNAG_NOTIFIER_VERSION = "Bugsnag-Notifier-Version"
        const val HEADER_BUGSNAG_DEVICE_ID = "Bugsnag-Device-Id"

        const val SECONDS_MS = 1000L

        const val DEFAULT_CONFIG_UPDATE_INTERVAL = 24 * 60 * 60 * SECONDS_MS
        const val DEFAULT_CONFIG_UPDATE_TOLERANCE = 2 * 60 * 60 * SECONDS_MS
        const val DEFAULT_CONFIG_EXPIRY_TIME =
            DEFAULT_CONFIG_UPDATE_INTERVAL + DEFAULT_CONFIG_UPDATE_TOLERANCE
        const val REQUEST_TIMEOUT_MS = 5_000

        /**
         * Hard upper bound on how long the caller waits for a single remote config request,
         * including DNS resolution and the TLS handshake, regardless of whether the socket
         * timeouts are honoured.
         */
        const val REQUEST_DEADLINE_MS = 15_000L

        /**
         * Requests run one at a time (in-flight dedupe plus the cross-process lock), so a second
         * thread is only used when an abandoned request is still blocked. Capping the pool stops
         * repeated DNS stalls from piling up threads; further requests fail fast instead.
         */
        const val MAX_REQUEST_THREADS = 2

        private const val API_KEY_VISIBLE_CHARS = 4

        private const val IDLE_THREAD_KEEP_ALIVE_MS = 30_000L

        internal val requestExecutor: ThreadPoolExecutor by lazy {
            ThreadPoolExecutor(
                0,
                MAX_REQUEST_THREADS,
                IDLE_THREAD_KEEP_ALIVE_MS,
                TimeUnit.MILLISECONDS,
                SynchronousQueue()
            ) { runnable ->
                Thread(runnable, "Bugsnag Remote Config Request").apply { isDaemon = true }
            }
        }
        val maxAgeRegex = Regex(""".*max-age\s*=\s*(\d+).*""")
    }
}

/** The request did not complete within [RemoteConfigRequest.REQUEST_DEADLINE_MS], or could not start. */
internal class RemoteConfigTimeoutException(message: String, cause: Throwable) : Exception(message, cause)
