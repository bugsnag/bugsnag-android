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
     * Options for stack sampled AppHang errors, when enabled potential AppHangs are identified
     * early (by a hang of [SamplingConfiguration.thresholdMillis]) and the monitored threads
     * stack is sampled repeatedly until either: the thread recovers, or an AppHang error is
     * raised. If a full AppHang error is reported in these cases, a secondary stack trace of
     * the most frequently seen stack path will be attached to the report.
     *
     * These error reports tend to group better than typical AppHangs and ANRs, and provide
     * more actionable insights at the expense of slowing down the application when it already
     * visibly stuck.
     *
     * The stack sampling is disabled by default.
     */
    var stackSampling: SamplingConfiguration? = null,
) {
    constructor() : this(DEFAULT_APP_HANG_THRESHOLD)

    class SamplingConfiguration(
        /**
         * How long after a heartbeat before the monitored thread should start being stack sampled.
         * Setting this to a value between 1 and [appHangThresholdMillis] enables stack sampling,
         * and produces a significantly higher-quality error at the expense of some runtime slowdown
         * and memory.
         *
         * A reasonable starting value is 1 second (`1000`) which is noticeable pause for a user,
         * but not long enough to trigger many false-positives.
         *
         * Defaults to 1000 - one second
         *
         * @see [intervalMillis]
         */
        var thresholdMillis: Long = DEFAULT_SAMPLING_THRESHOLD,
        /**
         * How many milliseconds to wait between stack samples. This is a best-effort value and the
         * real sampling rate may be different.
         *
         * Defaults to 50
         */
        var intervalMillis: Long = DEFAULT_SAMPLING_INTERVAL,
    ) {
        constructor() : this(DEFAULT_SAMPLING_THRESHOLD)
    }

    internal companion object {
        internal const val DEFAULT_APP_HANG_THRESHOLD: Long = 3000L
        internal const val DEFAULT_SAMPLING_THRESHOLD: Long = 1000L
        internal const val DEFAULT_SAMPLING_INTERVAL: Long = 50L
    }
}
