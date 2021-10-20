package com.bugsnag.android.internal.journal

import java.math.BigDecimal
import java.math.BigInteger
import java.util.HashMap

/**
 * "Normalize" a map by changing all numeric types to their largest forms.
 * This is necessary for comparing the results of serialization/deserialization
 * operations because we have no control over what types the codec will choose,
 * and equals() takes into account the underlying type.
 *
 * @param map The map to normalize
 * @param <K> The key type
 * @param <V> The value type
 * @return The normalized map
</V></K> */
@Suppress("UNCHECKED_CAST")
private fun <K, V> normalizedMap(map: Map<K, V>): Map<K, V> {
    val newMap: MutableMap<K, V> = HashMap(map.size)
    map.entries.forEach { entry ->
        var key = entry.key
        val normalizedKey = normalized(key as Any) as K
        if (key != normalizedKey) {
            key = normalizedKey
        }
        newMap[key] = normalized(entry.value as Any) as V
    }
    return newMap
}

/**
 * "Normalize" a list by changing all numeric types to their largest forms.
 * This is necessary for comparing the results of serialization/deserialization
 * operations because we have no control over what types the codec will choose,
 * and equals() takes into account the underlying type.
 *
 * @param list The list to normalize
 * @param <T> The element type
 * @return The normalized list
</T> */
@Suppress("UNCHECKED_CAST")
private fun <T> normalizedList(list: List<T>): List<T> = list.map { entry ->
    normalized(entry as Any) as T
}

/**
 * "Normalize" an unknown value by changing all numeric types to their largest forms.
 * This is necessary for comparing the results of serialization/deserialization
 * operations because we have no control over what types the codec will choose,
 * and equals() takes into account the underlying type.
 *
 * This function normalizes integers, floats, lists, and maps and their subobjects.
 *
 * @param obj The object to normalize.
 * @return The normalized object (may be the same object passed in)
 */
@Suppress("UNCHECKED_CAST")
fun <T> normalized(obj: Any): T {
    return when (obj) {
        is Byte -> obj.toLong()
        is Short -> obj.toLong()
        is Int -> obj.toLong()
        is Float -> normalizeFloat(obj)
        is BigInteger -> obj.toLong()
        is BigDecimal -> normalizeBigDecimal(obj)
        is Map<*, *> -> normalizedMap(obj as Map<Any, Any>)
        is List<*> -> normalizedList(obj as List<Any>)
        else -> obj
    } as T
}

private fun normalizeBigDecimal(obj: BigDecimal): Any = when {
    obj.toDouble() - obj.toLong() == 0.0 -> obj.toLong()
    else -> obj.toDouble()
}

private fun normalizeFloat(obj: Float): Any = when {
    obj.toDouble() - obj.toLong() == 0.0 -> obj.toLong()
    else -> obj.toDouble()
}
