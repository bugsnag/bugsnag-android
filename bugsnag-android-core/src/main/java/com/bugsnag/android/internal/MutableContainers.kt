package com.bugsnag.android.internal

/**
 * Deep convert containers to their mutable counterparts.
 */
internal fun Any?.bsgToMutableContainersDeep(): Any? = when (this) {
    is Map<*, *> -> {
        val mutableMap = this.toMutableMap()
        this.forEach {
            val original = it.value
            val mutable = original.bsgToMutableContainersDeep()
            if (mutable !== original) {
                mutableMap[it.key] = mutable
            }
        }
        mutableMap
    }
    is Collection<*> -> {
        val mutableList = this.toMutableList()
        val it = mutableList.listIterator()
        while (it.hasNext()) {
            val original = it.next()
            val mutable = original.bsgToMutableContainersDeep()
            if (mutable !== original) {
                it.set(mutable)
            }
        }
        mutableList
    }
    is Array<*> -> {
        val mutableList = this.toMutableList()
        val it = mutableList.listIterator()
        while (it.hasNext()) {
            val original = it.next()
            val mutable = original.bsgToMutableContainersDeep()
            if (mutable !== original) {
                it.set(mutable)
            }
        }
        mutableList
    }
    else -> this
}
