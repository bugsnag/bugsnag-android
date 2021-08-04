package com.bugsnag.android.internal

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Deep convert containers to their concurrent counterparts.
 * Note: Since ConcurrentHashMap cannot store null, this method will omit any keys that map to null!
 */
internal fun Any?.asConcurrent(): Any? = when (this) {
    is Map<*, *> -> {
        @Suppress("UNCHECKED_CAST")
        val concurrentMap = this as? ConcurrentMap<Any?, Any?> ?: ConcurrentHashMap()

        this.forEach {
            when (val entry = it.value) {
                null -> {} // ConcurrentHashMap cannot store null, so omit it.
                else -> concurrentMap[it.key] = entry.asConcurrent()
            }
        }
        concurrentMap
    }
    is Collection<*> -> {
        @Suppress("UNCHECKED_CAST")
        val concurrentList = this as? CopyOnWriteArrayList<Any?> ?: CopyOnWriteArrayList(this)

        for (i in concurrentList.indices) {
            concurrentList[i] = concurrentList[i].asConcurrent()
        }
        concurrentList
    }
    is Array<*> -> {
        @Suppress("UNCHECKED_CAST")
        val concurrentList = this as? CopyOnWriteArrayList<Any?> ?: CopyOnWriteArrayList(this)

        for (i in concurrentList.indices) {
            concurrentList[i] = concurrentList[i].asConcurrent()
        }
        concurrentList
    }
    else -> this
}
