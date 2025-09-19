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
        connection.setRequestProperty("Bugsnag-Api-Key", config.apiKey)
        connection.setRequestProperty("Bugsnag-Notifier-Name", notifier.name)
        connection.setRequestProperty("Bugsnag-Notifier-Version", notifier.version)

        if (remoteConfig != null) {
            connection.setRequestProperty("If-None-Match", remoteConfig.configurationTag)
        }

        val responseCode = connection.responseCode
        return when (responseCode) {
            200 -> parseRemoteConfig(connection)
            304 -> renewExistingConfig(configExpiryDate(connection))
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

        // Add binary architecture
        getPrimaryBinaryArch()?.let { binaryArch ->
            append("&binaryArch=")
            append(binaryArch)
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
        val tag = connection.getHeaderField("etag")

        json["configurationTag"] = tag
        json["configurationExpiry"] = DateUtils.toIso8601(expiryDate)

        return RemoteConfig.fromMap(json)
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
        val cacheControl = connection.getHeaderField("cache-control")
            ?: return defaultConfigExpiry()

        val maxAgeMatcher = maxAgeRegex.matchEntire(cacheControl)
            ?: return defaultConfigExpiry()
        val maxAge = maxAgeMatcher.groupValues.getOrNull(1)?.toLongOrNull()
            ?: return defaultConfigExpiry()

        return Date(System.currentTimeMillis() + (maxAge * 1000L))
    }

    private fun defaultConfigExpiry(): Date =
        Date(System.currentTimeMillis() + DEFAULT_CONFIG_EXPIRY_TIME)

    private fun getPrimaryBinaryArch(): String? {
        @Suppress("DEPRECATION")
        val primaryAbi = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                Build.SUPPORTED_ABIS.firstOrNull()
            }

            else -> Build.CPU_ABI
        }

        return when (primaryAbi) {
            "armeabi-v7a", "armeabi" -> "arm32"
            "arm64-v8a" -> "arm64"
            "x86" -> "x86"
            "x86_64" -> "x86_64"
            else -> primaryAbi // Return original value if not in our mapping
        }
    }

    companion object {
        const val DEFAULT_CONFIG_EXPIRY_TIME = 24 * 60 * 60 * 1000
        val maxAgeRegex = Regex(""".*max-age\s*=\s*(\d+).*""")
    }
}
