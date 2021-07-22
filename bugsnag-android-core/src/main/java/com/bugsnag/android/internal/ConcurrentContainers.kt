package com.bugsnag.android.internal

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Deep convert containers to their concurrent counterparts.
 */
internal fun Any?.asConcurrent(): Any? = when (this) {
    is Map<*, *> -> {
        @Suppress("UNCHECKED_CAST")
        val concurrentMap = this as? ConcurrentMap<Any?, Any?> ?: ConcurrentHashMap(this)

        this.forEach {
            when (val entry = it.value) {
                is Map<*, *> -> concurrentMap[it.key] = entry.asConcurrent()
                is List<*> -> concurrentMap[it.key] = entry.asConcurrent()
            }
        }
        concurrentMap
    }
    is List<*> -> {
        @Suppress("UNCHECKED_CAST")
        val concurrentList = this as? CopyOnWriteArrayList<Any?> ?: CopyOnWriteArrayList(this)

        for (i in this.indices) {
            when (val entry = this[i]) {
                is Map<*, *> -> concurrentList[i] = entry.asConcurrent()
                is List<*> -> concurrentList[i] = entry.asConcurrent()
            }
        }
        concurrentList
    }
    else -> this
}
