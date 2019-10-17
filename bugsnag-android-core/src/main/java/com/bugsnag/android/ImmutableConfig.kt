package com.bugsnag.android

import java.util.Date
import java.util.HashMap

internal data class ImmutableConfig(
    val apiKey: String,
    val autoDetectErrors: Boolean,
    val autoDetectAnrs: Boolean,
    val autoDetectNdkCrashes: Boolean,
    val autoTrackSessions: Boolean,
    val autoCaptureBreadcrumbs: Boolean,
    val sendThreads: Boolean,
    val ignoreClasses: Collection<String>,
    val enabledReleaseStages: Collection<String>,
    val projectPackages: Collection<String>,
    val enabledBreadcrumbTypes: Set<BreadcrumbType>,
    val releaseStage: String?,
    val buildUuid: String?,
    val appVersion: String?,
    val versionCode: Int,
    val codeBundleId: String?,
    val appType: String,
    val delivery: Delivery,
    val endpoints: Endpoints,
    val persistUserBetweenSessions: Boolean,
    val launchCrashThresholdMs: Long,
    val loggingEnabled: Boolean,
    val maxBreadcrumbs: Int
) {

    companion object {
        private const val HEADER_API_PAYLOAD_VERSION = "Bugsnag-Payload-Version"
        private const val HEADER_API_KEY = "Bugsnag-Api-Key"
        private const val HEADER_BUGSNAG_SENT_AT = "Bugsnag-Sent-At"
    }

    /**
     * Checks if the given release stage should be notified or not
     *
     * @param releaseStage the release stage to check
     * @return true if the release state should be notified else false
     */
    @JvmName("shouldNotifyForReleaseStage")
    internal fun shouldNotifyForReleaseStage() = enabledReleaseStages.isEmpty() || enabledReleaseStages.contains(releaseStage)

    @JvmName("errorApiDeliveryParams")
    internal fun errorApiDeliveryParams() = DeliveryParams(endpoints.notify, errorApiHeaders())

    @JvmName("sessionApiDeliveryParams")
    internal fun sessionApiDeliveryParams() = DeliveryParams(endpoints.sessions, sessionApiHeaders())

    /**
     * Supplies the headers which must be used in any request sent to the Error Reporting API.
     *
     * @return the HTTP headers
     */
    private fun errorApiHeaders(): Map<String, String> {
        val map = HashMap<String, String>()
        map[HEADER_API_PAYLOAD_VERSION] = "4.0"
        map[HEADER_API_KEY] = apiKey
        map[HEADER_BUGSNAG_SENT_AT] = DateUtils.toIso8601(Date())
        return map
    }

    /**
     * Supplies the headers which must be used in any request sent to the Session Tracking API.
     *
     * @return the HTTP headers
     */
    private fun sessionApiHeaders(): Map<String, String> {
        val map = HashMap<String, String>()
        map[HEADER_API_PAYLOAD_VERSION] = "1.0"
        map[HEADER_API_KEY] = apiKey
        map[HEADER_BUGSNAG_SENT_AT] = DateUtils.toIso8601(Date())
        return map
    }

}

internal fun convertToImmutableConfig(config: Configuration): ImmutableConfig {
    return ImmutableConfig(
        apiKey = config.apiKey,
        autoDetectErrors = config.autoDetectErrors,
        autoDetectAnrs = config.autoDetectAnrs,
        autoDetectNdkCrashes = config.autoDetectNdkCrashes,
        autoTrackSessions = config.autoTrackSessions,
        autoCaptureBreadcrumbs = config.autoCaptureBreadcrumbs,
        sendThreads = config.sendThreads,
        ignoreClasses = config.ignoreClasses.toSet(),
        enabledReleaseStages = config.enabledReleaseStages.toSet(),
        projectPackages = config.projectPackages.toSet(),
        releaseStage = config.releaseStage,
        buildUuid = config.buildUuid,
        appVersion = config.appVersion,
        versionCode = config.versionCode!!,
        codeBundleId = config.codeBundleId,
        appType = config.appType,
        delivery = config.delivery,
        endpoints = config.endpoints,
        persistUserBetweenSessions = config.persistUserBetweenSessions,
        launchCrashThresholdMs = config.launchCrashThresholdMs,
        loggingEnabled = config.loggingEnabled,
        maxBreadcrumbs = config.maxBreadcrumbs,
        enabledBreadcrumbTypes = config.enabledBreadcrumbTypes.toSet()
    )
}
