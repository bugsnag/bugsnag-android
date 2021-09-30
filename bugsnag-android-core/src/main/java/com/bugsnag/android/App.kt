package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.journal.Journalable
import java.io.IOException

/**
 * Stateless information set by the notifier about your app can be found on this class. These values
 * can be accessed and amended if necessary.
 */
open class App internal constructor(
    data: MutableMap<String, Any?> = mutableMapOf()
) : JsonStream.Streamable, Journalable {

    protected val map: MutableMap<String, Any?> = data.withDefault { null }

    /**
     * The architecture of the running application binary
     */
    var binaryArch: String? by map

    /**
     * The package name of the application
     */
    var id: String? by map

    /**
     * The release stage set in [Configuration.releaseStage]
     */
    var releaseStage: String? by map

    /**
     * The version of the application set in [Configuration.version]
     */
    var version: String? by map

    /**
     The revision ID from the manifest (React Native apps only)
     */
    var codeBundleId: String? by map

    /**
     * The unique identifier for the build of the application set in [Configuration.buildUuid]
     */
    var buildUuid: String? by map

    /**
     * The application type set in [Configuration#version]
     */
    var type: String? by map

    /**
     * The version code of the application set in [Configuration.versionCode]
     */
    var versionCode: Number by map

    internal constructor(
        binaryArch: String?,
        id: String?,
        releaseStage: String?,
        version: String?,
        codeBundleId: String?,
        buildUuid: String?,
        type: String?,
        versionCode: Number?
    ) : this(
        mutableMapOf(
            "binaryArch" to binaryArch,
            "id" to id,
            "releaseStage" to releaseStage,
            "version" to version,
            "codeBundleId" to codeBundleId,
            "buildUuid" to buildUuid,
            "type" to type,
            "versionCode" to versionCode
        )
    )

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

    override fun toJournalSection(): Map<String, Any?> {
        val copy = map.toMutableMap()
        copy["buildUUID"] = copy["buildUuid"]
        copy.remove("buildUuid")
        return copy
    }
}
