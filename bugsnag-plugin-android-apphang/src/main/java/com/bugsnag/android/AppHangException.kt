package com.bugsnag.android

/**
 * Exception type to indicate AppHangs. Created with a specific stackTrace (typically captured
 * from the main thread) instead of capturing the current stackTrace.
 */
class AppHangException(
    message: String,
    private val stackTrace: Array<out StackTraceElement>
) : RuntimeException(message) {
    override fun getStackTrace(): Array<out StackTraceElement?> = stackTrace
    override fun fillInStackTrace(): Throwable = this
}
