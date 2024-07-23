package com.bugsnag.android

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class ValueFuture<V>(private val value: V) : Future<V> {
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
    override fun isCancelled(): Boolean = false
    override fun isDone(): Boolean = true
    override fun get(): V = value
    override fun get(timeout: Long, unit: TimeUnit?): V = get()
}
