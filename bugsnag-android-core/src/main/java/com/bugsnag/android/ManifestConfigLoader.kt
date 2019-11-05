package com.bugsnag.android

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle

import androidx.annotation.VisibleForTesting

internal class ManifestConfigLoader {

    companion object {
        // mandatory
        private const val BUGSNAG_NS = "com.bugsnag.android"
        private const val API_KEY = "$BUGSNAG_NS.API_KEY"
        internal const val BUILD_UUID = "$BUGSNAG_NS.BUILD_UUID"

        // detection
        private const val AUTO_DETECT_ERRORS = "$BUGSNAG_NS.AUTO_DETECT_ERRORS"
        private const val AUTO_DETECT_ANRS = "$BUGSNAG_NS.AUTO_DETECT_ANRS"
        private const val AUTO_DETECT_NDK_CRASHES = "$BUGSNAG_NS.AUTO_DETECT_NDK_CRASHES"
        private const val AUTO_CAPTURE_SESSIONS = "$BUGSNAG_NS.AUTO_CAPTURE_SESSIONS"
        private const val SEND_THREADS = "$BUGSNAG_NS.SEND_THREADS"
        private const val PERSIST_USER = "$BUGSNAG_NS.PERSIST_USER_BETWEEN_SESSIONS"

        // endpoints
        private const val ENDPOINT_NOTIFY = "$BUGSNAG_NS.ENDPOINT"
        private const val ENDPOINT_SESSIONS = "$BUGSNAG_NS.SESSIONS_ENDPOINT"

        // app/project packages
        private const val APP_VERSION = "$BUGSNAG_NS.APP_VERSION"
        private const val VERSION_CODE = "$BUGSNAG_NS.VERSION_CODE"
        private const val RELEASE_STAGE = "$BUGSNAG_NS.RELEASE_STAGE"
        private const val ENABLED_RELEASE_STAGES = "$BUGSNAG_NS.ENABLED_RELEASE_STAGES"
        private const val IGNORE_CLASSES = "$BUGSNAG_NS.IGNORE_CLASSES"
        private const val PROJECT_PACKAGES = "$BUGSNAG_NS.PROJECT_PACKAGES"
        private const val REDACT_KEYS = "$BUGSNAG_NS.REDACT_KEYS"

        // misc
        private const val MAX_BREADCRUMBS = "$BUGSNAG_NS.MAX_BREADCRUMBS"
        private const val LAUNCH_CRASH_THRESHOLD_MS = "$BUGSNAG_NS.LAUNCH_CRASH_THRESHOLD_MS"

        // deprecated aliases
        private const val ENABLE_EXCEPTION_HANDLER = "$BUGSNAG_NS.ENABLE_EXCEPTION_HANDLER"
    }

    fun load(ctx: Context): Configuration {
        try {
            val packageManager = ctx.packageManager
            val packageName = ctx.packageName
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val data = ai.metaData
            return load(data)
        } catch (ignore: Exception) {
            throw IllegalStateException("Bugsnag is unable to read config from manifest.")
        }
    }

    /**
     * Populates the config with meta-data values supplied from the manifest as a Bundle.
     *
     * @param data   the manifest bundle
     */
    @VisibleForTesting
    internal fun load(data: Bundle): Configuration {
        val apiKey = data.getString(API_KEY)
            ?: throw IllegalArgumentException("You must provide a Bugsnag API key")
        val config = Configuration(apiKey)

        loadDetectionConfig(config, data)
        loadEndpointsConfig(config, data)
        loadAppConfig(config, data)

        // misc config
        with(config) {
            buildUuid = data.getString(BUILD_UUID)
            maxBreadcrumbs = data.getInt(MAX_BREADCRUMBS, maxBreadcrumbs)
            launchCrashThresholdMs =
                data.getInt(LAUNCH_CRASH_THRESHOLD_MS, launchCrashThresholdMs.toInt()).toLong()
        }
        return config
    }

    private fun loadDetectionConfig(config: Configuration, data: Bundle) {
        with(config) {
            autoDetectErrors = data.getBoolean(ENABLE_EXCEPTION_HANDLER, autoDetectErrors)
            autoDetectErrors = data.getBoolean(AUTO_DETECT_ERRORS, autoDetectErrors)
            autoDetectAnrs = data.getBoolean(AUTO_DETECT_ANRS, autoDetectAnrs)
            autoDetectNdkCrashes = data.getBoolean(AUTO_DETECT_NDK_CRASHES, autoDetectNdkCrashes)
            autoTrackSessions = data.getBoolean(AUTO_CAPTURE_SESSIONS, autoTrackSessions)
            sendThreads = data.getBoolean(SEND_THREADS, sendThreads)
            persistUserBetweenSessions = data.getBoolean(PERSIST_USER, persistUserBetweenSessions)
        }
    }

    private fun loadEndpointsConfig(config: Configuration, data: Bundle) {
        if (data.containsKey(ENDPOINT_NOTIFY)) {
            val endpoint = data.getString(ENDPOINT_NOTIFY, config.endpoints.notify)
            val sessionEndpoint = data.getString(ENDPOINT_SESSIONS, config.endpoints.sessions)
            config.endpoints = Endpoints(endpoint, sessionEndpoint)
        }
    }

    private fun loadAppConfig(config: Configuration, data: Bundle) {
        with(config) {
            releaseStage = data.getString(RELEASE_STAGE, config.releaseStage)
            appVersion = data.getString(APP_VERSION, config.appVersion)

            if (data.containsKey(VERSION_CODE)) {
                versionCode = data.getInt(VERSION_CODE)
            }
            enabledReleaseStages = getStrArray(data, ENABLED_RELEASE_STAGES, enabledReleaseStages).toSet()
            ignoreClasses = getStrArray(data, IGNORE_CLASSES, ignoreClasses).toSet()
            projectPackages = getStrArray(data, PROJECT_PACKAGES, projectPackages).toSet()
            redactKeys = getStrArray(data, REDACT_KEYS, redactKeys).toSet()
        }
    }

    private fun getStrArray(data: Bundle, key: String,
                            default: Collection<String>): Collection<String> {
        val delimitedStr = data.getString(key)

        return when (val ary = delimitedStr?.split(",")) {
            null -> default
            else -> ary.toSet()
        }
    }

}
