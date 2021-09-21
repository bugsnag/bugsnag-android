package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.JournalKeys

/**
 * Stateful information set by the notifier about your app can be found on this class. These values
 * can be accessed and amended if necessary.
 */
class AppWithState(
    binaryArch: String?,
    id: String?,
    releaseStage: String?,
    version: String?,
    codeBundleId: String?,
    buildUuid: String?,
    type: String?,
    versionCode: Number?,

    /**
     * The number of milliseconds the application was running before the event occurred
     */
    var duration: Number?,

    /**
     * The number of milliseconds the application was running in the foreground before the
     * event occurred
     */
    var durationInForeground: Number?,

    /**
     * Whether the application was in the foreground when the event occurred
     */
    var inForeground: Boolean?,

    /**
     * Whether the application was launching when the event occurred
     */
    var isLaunching: Boolean?
) : App(binaryArch, id, releaseStage, version, codeBundleId, buildUuid, type, versionCode) {

    internal constructor(
        config: ImmutableConfig,
        binaryArch: String?,
        id: String?,
        releaseStage: String?,
        version: String?,
        codeBundleId: String?,
        duration: Number?,
        durationInForeground: Number?,
        inForeground: Boolean?,
        isLaunching: Boolean?
    ) : this(
        binaryArch,
        id,
        releaseStage,
        version,
        codeBundleId,
        config.buildUuid,
        config.appType,
        config.versionCode,
        duration,
        durationInForeground,
        inForeground,
        isLaunching
    )

    override fun toJournalSection(): Map<String, Any?> = super.toJournalSection().plus(
        mapOf(
            JournalKeys.keyDuration to duration,
            JournalKeys.keyDurationInFG to durationInForeground,
            JournalKeys.keyInForeground to inForeground,
            JournalKeys.keyIsLaunching to isLaunching
        )
    )
}
