package com.bugsnag.android.internal.remoteconfig

/**
 * Wall-clock source for remote config expiry and backoff decisions. Production always uses
 * [SYSTEM]; tests and the fleet simulator substitute an accelerated clock so that multi-hour
 * backoff schedules can be exercised in minutes.
 */
internal fun interface RemoteConfigClock {
    fun currentTimeMillis(): Long

    companion object {
        @JvmField
        val SYSTEM = RemoteConfigClock { System.currentTimeMillis() }
    }
}
