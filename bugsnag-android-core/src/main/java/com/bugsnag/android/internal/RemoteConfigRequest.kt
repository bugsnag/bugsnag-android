package com.bugsnag.android.internal

import android.os.Build
import com.bugsnag.android.Notifier
import com.bugsnag.android.RemoteConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Date

internal class RemoteConfigRequest(
    private val baseUrl: String,
    private val remoteConfig: RemoteConfig?,
    private val config: ImmutableConfig,
    private val notifier: Notifier,
    private val onRemoteConfigUpdate: (RemoteConfig) -> Unit
) : Runnable {
    override fun run() {
        val newRemoteConfig: RemoteConfig? = try {
            requestNewConfig()
        } catch (_: Exception) {
            // if we fail to retrieve the RemoteConfig, we retry exactly once
            try {
                requestNewConfig()
            } catch (ex: Exception) {
                config.logger.d("Could not retrieve RemoteConfig", ex)
            }
            null
        }

        if (newRemoteConfig != null) {
            onRemoteConfigUpdate(newRemoteConfig)
        }
    }

    private fun requestNewConfig(): RemoteConfig? {
        val urlWithParams = buildUrlWithQueryParameters()

        val url = URL(urlWithParams)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.doOutput = false

        // Set required headers
        connection.setRequestProperty(HEADER_BUGSNAG_API_KEY, config.apiKey)
        connection.setRequestProperty(HEADER_BUGSNAG_NOTIFIER_NAME, notifier.name)
        connection.setRequestProperty(HEADER_BUGSNAG_NOTIFIER_VERSION, notifier.version)

        if (remoteConfig != null) {
            connection.setRequestProperty(HEADER_IF_NONE_MATCH, remoteConfig.configurationTag)
        }

        val responseCode = connection.responseCode
        return when (responseCode) {
            HttpURLConnection.HTTP_OK -> parseRemoteConfig(connection)
            HttpURLConnection.HTTP_NOT_MODIFIED -> renewExistingConfig(configExpiryDate(connection))
            else -> null
        }
    }

    private fun buildUrlWithQueryParameters(): String = buildString {
        append(baseUrl)

        // Add osVersion (required)
        append("?osVersion=")
        append(Build.VERSION.SDK_INT)

        // Add optional parameters
        config.appVersion?.let { version ->
            append("&version=")
            append(URLEncoder.encode(version, StandardCharsets.UTF_8))
        }

        config.versionCode?.let { versionCode ->
            append("&versionCode=")
            append(versionCode)
        }

        config.releaseStage?.let { releaseStage ->
            append("&releaseStage=")
            append(URLEncoder.encode(releaseStage, StandardCharsets.UTF_8))
        }

        // Add app ID (package name)
        config.packageInfo?.packageName?.let { appId ->
            append("&appId=")
            append(URLEncoder.encode(appId, StandardCharsets.UTF_8))
        }
    }

    private fun parseRemoteConfig(connection: HttpURLConnection): RemoteConfig? {
        val inputStream = connection.inputStream

        @Suppress("UNCHECKED_CAST")
        val json = JsonCollectionParser(inputStream).parse()
                as? LinkedHashMap<String, Any?>
            ?: return null

        val expiryDate = configExpiryDate(connection)
        val tag = connection.getHeaderField(HEADER_ETAG)

        return RemoteConfig.fromMap(json, tag, expiryDate)
    }

    private fun renewExistingConfig(configExpiryDate: Date): RemoteConfig? {
        if (remoteConfig == null) {
            return null
        }

        return RemoteConfig(
            remoteConfig.configurationTag,
            configExpiryDate,
            remoteConfig.discardRules
        )
    }

    private fun configExpiryDate(connection: HttpURLConnection): Date {
        val cacheControl = connection.getHeaderField(HEADER_CACHE_CONTROL)
            ?: return defaultConfigExpiry()

        val maxAgeMatcher = maxAgeRegex.matchEntire(cacheControl)
            ?: return defaultConfigExpiry()
        val maxAgeSeconds = maxAgeMatcher.groupValues.getOrNull(1)?.toLongOrNull()
            ?: return defaultConfigExpiry()

        return Date(System.currentTimeMillis() + (maxAgeSeconds * 1000L))
    }

    private fun defaultConfigExpiry(): Date =
        Date(System.currentTimeMillis() + DEFAULT_CONFIG_EXPIRY_TIME)

    internal companion object {
        const val HEADER_ETAG = "ETag"
        const val HEADER_CACHE_CONTROL = "Cache-Control"
        const val HEADER_IF_NONE_MATCH = "If-None-Match"
        const val HEADER_BUGSNAG_NOTIFIER_NAME = "Bugsnag-Notifier-Name"
        const val HEADER_BUGSNAG_NOTIFIER_VERSION = "Bugsnag-Notifier-Version"

        const val DEFAULT_CONFIG_EXPIRY_TIME = 24 * 60 * 60 * 1000
        val maxAgeRegex = Regex(""".*max-age\s*=\s*(\d+).*""")
    }
}
