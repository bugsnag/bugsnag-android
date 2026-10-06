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
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Date

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
    private val logger: Logger
) {
    constructor(
        config: ImmutableConfig,
        notifier: Notifier,
        currentRemoteConfig: RemoteConfig?
    ) : this(
        config.endpoints.configuration,
        config.apiKey,
        notifier,
        config.appVersion ?: config.packageInfo?.versionName,
        config.versionCode,
        config.releaseStage,
        config.packageInfo?.packageName,
        currentRemoteConfig,
        config.logger
    )

    fun requestConfig(): RemoteConfig? {
        if (baseUrl == null) {
            logger.d("Skipping remote config request because the configuration endpoint is not configured")
            return null
        }

        return try {
            requestNewConfig()
        } catch (ex: Exception) {
            logger.w("Remote config request failed; returning null to start cooldown", ex)
            null
        }
    }

    private fun requestNewConfig(): RemoteConfig? {
        val urlWithParams = buildUrlWithQueryParameters() ?: return null

        logger.i(
            "Requesting remote config from $urlWithParams " +
                "cachedTag=${remoteConfig?.configurationTag ?: "<none>"}"
        )

        val url = URL(urlWithParams)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.doOutput = false

        // Set required headers
        connection.setRequestProperty(HEADER_BUGSNAG_API_KEY, apiKey)
        connection.setRequestProperty(HEADER_BUGSNAG_NOTIFIER_NAME, notifier.name)
        connection.setRequestProperty(HEADER_BUGSNAG_NOTIFIER_VERSION, notifier.version)

        if (remoteConfig?.configurationTag != null) {
            connection.setRequestProperty(HEADER_IF_NONE_MATCH, remoteConfig.configurationTag)
            logger.d("Sending remote config request with If-None-Match=${remoteConfig.configurationTag}")
        } else {
            logger.d("Sending remote config request without If-None-Match header")
        }

        val responseCode = connection.responseCode
        logger.i(
            "Remote config response received code=$responseCode message=${connection.responseMessage} " +
                "etag=${connection.getHeaderField(HEADER_ETAG)} " +
                "cacheControl=${connection.getHeaderField(HEADER_CACHE_CONTROL)} " +
                "contentLength=${connection.contentLength}"
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
    }

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

        val inputStream = connection.inputStream

        val parser = try {
            JsonCollectionParser(inputStream)
        } catch (_: JsonParseException) {
            // these can happen when the response is empty, but the Content-Length was not set
            logger.i(
                "Remote config response body was empty or malformed; returning empty config " +
                    "tag=${tag ?: "<null>"} expiry=${expiryDate.time}"
            )
            return RemoteConfig(tag, expiryDate, emptyList())
        }

        @Suppress("UNCHECKED_CAST")
        val json = parser.parse()
            as? LinkedHashMap<String, Any?>
            ?: return null

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

        if (maxAgeSeconds <= 0L) {
            return defaultConfigExpiry("non-positive max-age value in Cache-Control header '$cacheControl'")
        }

        val now = System.currentTimeMillis()
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
        val expiry = Date(System.currentTimeMillis() + DEFAULT_CONFIG_EXPIRY_TIME)
        logger.d("Using default remote config expiry because $reason -> ${expiry.time}")
        return expiry
    }

    internal companion object {
        const val HEADER_ETAG = "ETag"
        const val HEADER_CACHE_CONTROL = "Cache-Control"
        const val HEADER_IF_NONE_MATCH = "If-None-Match"
        const val HEADER_BUGSNAG_NOTIFIER_NAME = "Bugsnag-Notifier-Name"
        const val HEADER_BUGSNAG_NOTIFIER_VERSION = "Bugsnag-Notifier-Version"

        const val SECONDS_MS = 1000L

        const val DEFAULT_CONFIG_UPDATE_INTERVAL = 24 * 60 * 60 * SECONDS_MS
        const val DEFAULT_CONFIG_UPDATE_TOLERANCE = 2 * 60 * 60 * SECONDS_MS
        const val DEFAULT_CONFIG_EXPIRY_TIME =
            DEFAULT_CONFIG_UPDATE_INTERVAL + DEFAULT_CONFIG_UPDATE_TOLERANCE
        val maxAgeRegex = Regex(""".*max-age\s*=\s*(\d+).*""")
    }
}
