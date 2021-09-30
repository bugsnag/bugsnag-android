package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig

/**
 * Stateful information set by the notifier about your app can be found on this class. These values
 * can be accessed and amended if necessary.
 */
class AppWithState internal constructor(
    data: MutableMap<String, Any?> = mutableMapOf()
) : App(data) {

    /**
     * The number of milliseconds the application was running before the event occurred
     */
    var duration: Number? by map

    /**
     * The number of milliseconds the application was running in the foreground before the
     * event occurred
     */
    var durationInForeground: Number? by map

    /**
     * Whether the application was in the foreground when the event occurred
     */
    var inForeground: Boolean? by map

    /**
     * Whether the application was launching when the event occurred
     */
    var isLaunching: Boolean? by map

    internal constructor(
        binaryArch: String?,
        id: String?,
        releaseStage: String?,
        version: String?,
        codeBundleId: String?,
        buildUuid: String?,
        type: String?,
        versionCode: Number?,
        duration: Number?,
        durationInForeground: Number?,
        inForeground: Boolean?,
        isLaunching: Boolean?
    ) : this(
        mutableMapOf(
            "binaryArch" to binaryArch,
            "id" to id,
            "releaseStage" to releaseStage,
            "version" to version,
            "codeBundleId" to codeBundleId,
            "buildUuid" to buildUuid,
            "type" to type,
            "versionCode" to versionCode,
            "duration" to duration,
            "durationInForeground" to durationInForeground,
            "inForeground" to inForeground,
            "isLaunching" to isLaunching
        )
    )

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
}
