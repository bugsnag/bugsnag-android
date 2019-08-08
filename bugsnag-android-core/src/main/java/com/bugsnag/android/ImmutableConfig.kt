package com.bugsnag.android

// TODO check whether these need to be null?

internal data class ImmutableConfig(
    val apiKey: String,
    val autoNotify: Boolean,
    val detectAnrs: Boolean,
    val detectNdkCrashes: Boolean,
    val autoCaptureSessions: Boolean,
    val autoCaptureBreadcrumbs: Boolean,
    val sendThreads: Boolean,
    val ignoreClasses: List<String>,
    val notifyReleaseStages: List<String>,
    val projectPackages: List<String>,
    val releaseStage: String?,
    val buildUuid: String?,
    val appVersion: String?,
    val codeBundleId: String?,
    val notifierType: String?,
    val delivery: Delivery,
    val endpoints: Endpoints,
    val persistUserBetweenSessions: Boolean,
    val launchCrashThresholdMs: Long,
    val loggingEnabled: Boolean,
    val maxBreadcrumbs: Int
)

internal fun convertToImmutableConfig(config: Configuration): ImmutableConfig {
    return ImmutableConfig(
        apiKey = config.apiKey,
        autoNotify = config.autoNotify,
        detectAnrs = config.detectAnrs,
        detectNdkCrashes = config.detectNdkCrashes,
        autoCaptureSessions = config.autoCaptureSessions,
        autoCaptureBreadcrumbs = config.autoCaptureBreadcrumbs,
        sendThreads = config.sendThreads,
        ignoreClasses = config.ignoreClasses?.toList() ?: emptyList(),
        notifyReleaseStages = config.notifyReleaseStages?.toList() ?: emptyList(),
        projectPackages = config.projectPackages?.toList() ?: emptyList(),
        releaseStage = config.releaseStage,
        buildUuid = config.buildUuid,
        appVersion = config.appVersion,
        codeBundleId = config.codeBundleId,
        notifierType = config.notifierType,
        delivery = config.delivery,
        endpoints = config.endpoints,
        persistUserBetweenSessions = config.persistUserBetweenSessions,
        launchCrashThresholdMs = config.launchCrashThresholdMs,
        loggingEnabled = config.loggingEnabled,
        maxBreadcrumbs = config.maxBreadcrumbs
    )
}
