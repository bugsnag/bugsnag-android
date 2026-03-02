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
    /**
     * How long after a heartbeat before the monitored thread should start being stack sampled.
     * Setting this to a value between 1 and [appHangThresholdMillis] enables stack sampling,
     * which produces a significantly higher-quality error at the expense of some runtime slowdown
     * and memory.
     *
     * When enabled, potential AppHangs are identified early and the monitored thread's stack is
     * sampled repeatedly until either the thread recovers or an AppHang error is raised. If a
     * full AppHang error is reported, a secondary stack trace of the most frequently seen stack
     * path will be attached to the report. These error reports tend to group better than typical
     * AppHangs and ANRs, and provide more actionable insights.
     *
     * A reasonable starting value is 1 second (`1000`) which is a noticeable pause for a user,
     * but not long enough to trigger many false-positives.
     *
     * Set to `null` to disable stack sampling (default).
     *
     * @see [stackSamplingIntervalMillis]
     */
    var stackSamplingThresholdMillis: Long? = null,
    /**
     * How many milliseconds to wait between stack samples. This is a best-effort value and the
     * real sampling rate may be different.
     *
     * This property only takes effect when [stackSamplingThresholdMillis] is set to a non-null
     * value to enable stack sampling.
     *
     * Defaults to 50
     */
    var stackSamplingIntervalMillis: Long = DEFAULT_SAMPLING_INTERVAL,
    /**
     * The cooldown period in milliseconds after an AppHang has been reported. During this period,
     * subsequent AppHangs will be suppressed to prevent over-reporting when the application is
     * running on very slow devices or under extreme conditions.
     *
     * For example, if set to 5000ms (5 seconds), after an AppHang is reported, any additional
     * AppHangs detected within the next 5 seconds will be ignored. This helps avoid flooding
     * error reports when a device is experiencing sustained performance issues.
     *
     * Set to 0 (default) to disable the cooldown period and report all detected AppHangs.
     */
    var appHangCooldownMillis: Long = 0L,
) {
    constructor() : this(DEFAULT_APP_HANG_THRESHOLD)

    internal companion object {
        internal const val DEFAULT_APP_HANG_THRESHOLD: Long = 3000L
        internal const val DEFAULT_SAMPLING_INTERVAL: Long = 50L
    }
}
