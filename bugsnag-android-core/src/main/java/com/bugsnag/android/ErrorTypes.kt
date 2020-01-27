package com.bugsnag.android

class ErrorTypes(

    /**
     * Sets whether [ANRs](https://developer.android.com/topic/performance/vitals/anr)
     * should be reported to Bugsnag. When enabled, Bugsnag will record an ANR whenever the main
     * thread has been blocked for 5000 milliseconds or longer.
     *
     * If you wish to enable ANR detection, you should set this property to true.
     */
    var anrs: Boolean = true,

    /**
     * Determines whether NDK crashes such as signals and exceptions should be reported by bugsnag.
     *
     * If you are using bugsnag-android this flag is false by default; if you are using
     * bugsnag-android-ndk this flag is true by default.
     */
    var ndkCrashes: Boolean = false,

    /**
     * Sets whether Bugsnag should automatically capture and report unhandled errors.
     * By default, this value is true.
     */
    var unhandledExceptions: Boolean = true
) {
    internal constructor(detectErrors: Boolean) : this(detectErrors, detectErrors, detectErrors)

    internal fun copy() = ErrorTypes(anrs, ndkCrashes, unhandledExceptions)
}
