package com.bugsnag.android.internal

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A directive determines how document path commands are interpreted. Making a container,
 * getting from a container, or setting to a container will require different operations to
 * complete, depending on the context.
 */
internal interface DocumentPathDirective<C> {

    /**
     * Make a new container at the current level.
     *
     * @return The new container.
     */
    fun newContainer(): C

    /**
     * Get an object from the container at the current level.
     *
     * @param container The container to get the object from.
     * @return The resulting object, or null if not found.
     */
    fun getFromContainer(container: C): Any?

    /**
     * Set an object in a container at the current level.
     *
     * @param container The container to set a value in.
     * @param value The value to set (can be null, which usually means "remove")
     */
    fun setInContainer(container: C, value: Any?)

    companion object {
        private fun convertToConcurrent(list: List<*>): CopyOnWriteArrayList<*> {
            val concurrentList = CopyOnWriteArrayList(list)
            for (i in list.indices) {
                val entry = list[i]
                if (entry is Map<*, *>) {
                    concurrentList[i] = convertToConcurrent(entry)
                } else if (entry is List<*>) {
                    concurrentList[i] = convertToConcurrent(entry)
                }
            }
            return concurrentList
        }

        private fun convertToConcurrent(map: Map<*, *>): ConcurrentMap<*, *> {
            val concurrentMap = ConcurrentHashMap(map)
            map.forEach {
                val entry = it.value
                if (entry is Map<*, *>) {
                    concurrentMap[it.key] = convertToConcurrent(entry)
                } else if (entry is List<*>) {
                    concurrentMap[it.key] = convertToConcurrent(entry)
                }
            }
            return concurrentMap
        }

        /**
         * Convert an arbitrary type to a concurrency safe version.
         * This only converts if it's a map or list, and simply returns the original value if
         * it's another type or null.
         */
        internal fun convertToConcurrent(obj: Any?): Any? {
            if (obj is Map<*, *>) {
                return convertToConcurrent(obj)
            }
            if (obj is List<*>) {
                return convertToConcurrent(obj)
            }
            return obj
        }
    }

    /**
     * Modifies a particular key in a map, or creates a new (String, Object) map.
     * Setting null removes the value.
     */
    class MapKeyDirective(private val key: String) : DocumentPathDirective<MutableMap<String, in Any>> {
        override fun newContainer(): MutableMap<String, in Any> {
            return ConcurrentHashMap()
        }

        override fun getFromContainer(container: MutableMap<String, in Any>): Any? {
            return container[key]
        }

        override fun setInContainer(container: MutableMap<String, in Any>, value: Any?) {
            val convertedValue = convertToConcurrent(value)
            if (convertedValue == null) {
                container.remove(key)
            } else {
                container[key] = convertedValue
            }
        }
    }

    /**
     * Modifies a list at the current level, or creates a new list.
     * Setting null removes the item at the specified index.
     */
    open class ListIndexDirective(private val index: Int) : DocumentPathDirective<MutableList<in Any>> {
        override fun newContainer(): MutableList<in Any> {
            return CopyOnWriteArrayList()
        }

        override fun getFromContainer(container: MutableList<in Any>): Any? {
            return container[index]
        }

        override fun setInContainer(container: MutableList<in Any>, value: Any?) {
            val convertedValue = convertToConcurrent(value)
            container.removeAt(index)
            if (convertedValue != null) {
                container.add(index, convertedValue)
            }
        }
    }

    /**
     * Special list directive for the last index in a list (-1).
     * This directive inserts a new entry if one isn't present already when setting.
     * Setting null deletes the last index (if present).
     */
    class ListLastIndexDirective : ListIndexDirective(0) {
        override fun getFromContainer(container: MutableList<in Any>): Any? {
            return container.lastOrNull()
        }

        override fun setInContainer(container: MutableList<in Any>, value: Any?) {
            val convertedValue = convertToConcurrent(value)
            if (container.isNotEmpty()) {
                container.removeAt(container.lastIndex)
            }
            if (convertedValue != null) {
                container.add(convertedValue)
            }
        }
    }

    /**
     * Special list directive for insert (path ends in .)
     * This one always inserts a new entry on set, and always returns null on get.
     * Setting null is an error.
     */
    class ListInsertDirective : ListIndexDirective(0) {
        override fun getFromContainer(container: MutableList<in Any>): Any? {
            return null
        }

        override fun setInContainer(container: MutableList<in Any>, value: Any?) {
            val convertedValue = convertToConcurrent(value)
            requireNotNull(convertedValue) { "Cannot use null for last path insert value" }
            container.add(convertedValue)
        }
    }
}
