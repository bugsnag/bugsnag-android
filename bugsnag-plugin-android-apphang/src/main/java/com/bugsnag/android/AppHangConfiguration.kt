package com.bugsnag.android

import android.os.Looper

/**
 * Configuration for the [BugsnagAppHangPlugin].
 */
class AppHangConfiguration @JvmOverloads constructor(
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
    /**
     * Once an AppHang is reported further AppHang errors will be suppressed until this "recovery
     * threshold" has been reached. Setting this to anything less than `appHangThresholdMillis * 2`
     * is the same as setting it to `0` (and all AppHangs will be reported). Typically this is
     * a significantly larger value than `appHangThresholdMillis`.
     *
     * Defaults to 0 - every AppHang will be reported
     */
    var recoveryTimeMillis: Long = DEFAULT_APP_HANG_RECOVERY_THRESHOLD
) {
    internal companion object {
        internal const val DEFAULT_APP_HANG_THRESHOLD: Long = 3000L
        internal const val DEFAULT_APP_HANG_RECOVERY_THRESHOLD: Long = 0L
    }
}
