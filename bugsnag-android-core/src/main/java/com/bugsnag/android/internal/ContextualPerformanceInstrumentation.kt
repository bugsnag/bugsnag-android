package com.bugsnag.android.internal

import com.bugsnag.android.PerformanceInstrumentation
import java.util.concurrent.atomic.AtomicInteger

class ContextualPerformanceInstrumentation<T>(
    val parentToken: T,
    private val delegate: PerformanceInstrumentation<T>
) : PerformanceInstrumentation<T> {
    private val childCount = AtomicInteger(0)

    fun onStart(name: String): T {
        return onStart(name, parentToken)
    }

    override fun onStart(name: String, parent: T?): T {
        childCount.incrementAndGet()
        return delegate.onStart(name, parent)
    }

    override fun onEnd(token: T) {
        delegate.onEnd(token)

        // we auto end the parent when all children are ended
        if (childCount.decrementAndGet() <= 0) {
            delegate.onEnd(parentToken)
        }
    }
}

object NoPerformanceInstrumentation : PerformanceInstrumentation<Any> {
    override fun onStart(name: String, parent: Any?): Any {
        return this
    }

    override fun onEnd(token: Any) {}
}
