package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.journal.JournalKeys
import com.bugsnag.android.internal.journal.Journalable
import java.io.IOException

/**
 * Stateless information set by the notifier about your app can be found on this class. These values
 * can be accessed and amended if necessary.
 */
open class App internal constructor(
    /**
     * The architecture of the running application binary
     */
    var binaryArch: String?,

    /**
     * The package name of the application
     */
    var id: String?,

    /**
     * The release stage set in [Configuration.releaseStage]
     */
    var releaseStage: String?,

    /**
     * The version of the application set in [Configuration.version]
     */
    var version: String?,

    /**
     The revision ID from the manifest (React Native apps only)
     */
    var codeBundleId: String?,

    /**
     * The unique identifier for the build of the application set in [Configuration.buildUuid]
     */
    var buildUuid: String?,

    /**
     * The application type set in [Configuration#version]
     */
    var type: String?,

    /**
     * The version code of the application set in [Configuration.versionCode]
     */
    var versionCode: Number?
) : JsonStream.Streamable, Journalable {

    internal constructor(
        config: ImmutableConfig,
        binaryArch: String?,
        id: String?,
        releaseStage: String?,
        version: String?,
        codeBundleId: String?
    ) : this(
        binaryArch,
        id,
        releaseStage,
        version,
        codeBundleId,
        config.buildUuid,
        config.appType,
        config.versionCode
    )

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) = writer.value(toJournalSection())

    override fun toJournalSection(): Map<String, Any?> = mapOf(
        JournalKeys.keyBinaryArch to binaryArch,
        JournalKeys.keyBuildUUID to buildUuid,
        JournalKeys.keyCodeBundleId to codeBundleId,
        JournalKeys.keyId to id,
        JournalKeys.keyReleaseStage to releaseStage,
        JournalKeys.keyType to type,
        JournalKeys.keyVersion to version,
        JournalKeys.keyVersionCode to versionCode
    )
}
