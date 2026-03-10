package com.bugsnag.android

interface PerformanceInstrumentation<T> {
    fun onStart(name: String, parent: T? = null): T
    fun onEnd(token: T)
}
