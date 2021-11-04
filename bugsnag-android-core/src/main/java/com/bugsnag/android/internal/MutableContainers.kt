package com.bugsnag.android.internal

import java.util.HashMap
import java.util.LinkedList
import kotlin.collections.LinkedHashMap

/**
 * Deep convert containers to their mutable counterparts.
 */
@Suppress("UNCHECKED_CAST")
internal fun Any?.bsgToMutableContainersDeep(): Any? = when (this) {
    is LinkedHashMap<*, *> -> convertMutableMapDescendents(this as MutableMap<Any, Any?>)
    is HashMap<*, *> -> convertMutableMapDescendents(this as MutableMap<Any, Any?>)
    is LinkedList<*> -> convertMutableListDescendents(this as MutableList<Any?>)
    is ArrayList<*> -> convertMutableListDescendents(this as MutableList<Any?>)
    is Map<*, *> -> convertImmutableMapAndDescendents(this as Map<Any, Any?>)
    is Collection<*> -> convertCollectionAndDescendents(this as Collection<Any?>)
    is Array<*> -> convertArrayAndDescendents(this as Array<Any?>)
    else -> this
}

private fun convertMutableMapDescendents(map: MutableMap<Any, Any?>): MutableMap<Any, Any?> {
    map.forEach {
        val original = it.value
        val mutable = original.bsgToMutableContainersDeep()
        if (mutable !== original) {
            map[it.key] = mutable
        }
    }
    return map
}

private fun convertMutableListDescendents(list: MutableList<Any?>): MutableList<Any?> {
    val it = list.listIterator()
    while (it.hasNext()) {
        val original = it.next()
        val mutable = original.bsgToMutableContainersDeep()
        if (mutable !== original) {
            it.set(mutable)
        }
    }

    return list
}

private fun convertImmutableMapAndDescendents(map: Map<Any, Any?>): MutableMap<Any, Any?> {
    val mutable = mutableMapOf<Any, Any?>()
    map.forEach {
        mutable[it.key] = it.value.bsgToMutableContainersDeep()
    }
    return mutable
}

private fun convertCollectionAndDescendents(collection: Collection<Any?>): MutableList<Any?> {
    val mutable = mutableListOf<Any?>()
    collection.forEach() {
        mutable.add(it.bsgToMutableContainersDeep())
    }
    return mutable
}

private fun convertArrayAndDescendents(array: Array<Any?>): MutableList<Any?> {
    val mutable = mutableListOf<Any?>()
    array.forEach() {
        mutable.add(it.bsgToMutableContainersDeep())
    }
    return mutable
}
