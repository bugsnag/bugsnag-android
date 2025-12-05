package com.bugsnag.android

import android.os.Looper

/**
 * Configuration for the [BugsnagAppHangPlugin].
 */
class AppHangConfiguration(
    /**
     * The maximum time between heartbeat messages being processed before an AppHang will be
     * reported.
     *
     * Defaults to 3 seconds.
     */
    var appHangThresholdMillis: Long = DEFAULT_APP_HANG_THRESHOLD,
    /**
     * The [Looper] being tracked for AppHang reporting.
     *
     * Defaults to [Looper.getMainLooper]
     */
    var watchedLooper: Looper = Looper.getMainLooper(),
) {
    constructor() : this(DEFAULT_APP_HANG_THRESHOLD)

    internal companion object {
        internal const val DEFAULT_APP_HANG_THRESHOLD: Long = 3000L
    }
}
