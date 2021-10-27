package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import com.bugsnag.android.internal.journal.JournalKeys
import java.util.Date

/**
 * Stateful information set by the notifier about the device on which the event occurred can be
 * found on this class. These values can be accessed and amended if necessary.
 */
class DeviceWithState internal constructor(
    data: MutableMap<String, Any?> = mutableMapOf(),
    runtimeVersions: MutableMap<String, Any>?
) : Device(data, runtimeVersions) {

    internal constructor(
        buildInfo: DeviceBuildInfo,
        jailbroken: Boolean?,
        id: String?,
        locale: String?,
        totalMemory: Long?,
        runtimeVersions: MutableMap<String, Any>,
        freeDisk: Long?,
        freeMemory: Long?,
        orientation: String?,
        time: Date?
    ) : this(
        mutableMapOf(
            "cpuAbi" to buildInfo.cpuAbis,
            "manufacturer" to buildInfo.manufacturer,
            "model" to buildInfo.model,
            "osName" to "android",
            "osVersion" to buildInfo.osVersion,
            "runtimeVersions" to runtimeVersions,
            "totalMemory" to totalMemory,
            "locale" to locale,
            "id" to id,
            "jailbroken" to jailbroken,
            "freeDisk" to freeDisk,
            "freeMemory" to freeMemory,
            "orientation" to orientation,
            "time" to time
        ),
        runtimeVersions
    )

    /**
     * The timestamp on the device when the event occurred
     */
    var time: Date? by map

    /**
     * The orientation of the device when the event occurred: either portrait or landscape
     */
    var orientation: String? by map

    /**
     * The number of free bytes of memory available on the device
     */
    var freeMemory: Long? by map

    /**
     * The number of free bytes of storage available on the device
     */
    var freeDisk: Long? by map

    override fun toJournalSection(): Map<String, Any?> = super.toJournalSection().plus(
        mapOf(
            JournalKeys.keyFreeDisk to freeDisk,
            JournalKeys.keyFreeMemory to freeMemory,
            JournalKeys.keyOrientation to orientation,
            JournalKeys.keyTime to time?.let(DateUtils::toIso8601)
        )
    )
}
