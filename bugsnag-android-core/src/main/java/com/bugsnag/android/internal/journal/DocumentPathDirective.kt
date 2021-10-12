package com.bugsnag.android.internal.journal

import com.bugsnag.android.internal.asConcurrent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A directive determines how document path commands are interpreted. Making a container,
 * getting from a container, or setting to a container will require different operations to
 * complete, depending on the context.
 */
internal interface DocumentPathDirective<C> {
    companion object {
        internal fun addNumbers(a: Number, b: Number): Number {
            if (a is Float || a is Double) {
                if (b is Float || b is Double) {
                    return a.toDouble() + b.toDouble()
                }
                return a.toDouble() + b.toLong()
            }
            if (b is Float || b is Double) {
                return a.toLong() + b.toDouble()
            }
            return a.toLong() + b.toLong()
        }
    }

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

    /**
     * Modifies a particular key in a map, or creates a new (String, Object) map.
     * Setting null removes the value.
     */
    open class MapKeyDirective(private val key: String) :
        DocumentPathDirective<MutableMap<String, in Any>> {
        override fun newContainer(): MutableMap<String, in Any> {
            return ConcurrentHashMap()
        }

        override fun getFromContainer(container: MutableMap<String, in Any>): Any? {
            return container[key]
        }

        override fun setInContainer(container: MutableMap<String, in Any>, value: Any?) {
            val concurrentValue = value.asConcurrent()
            if (concurrentValue == null) {
                container.remove(key)
            } else {
                container[key] = concurrentValue
            }
        }

        override fun toString(): String {
            return "MapKeyDirective(key='$key')"
        }
    }

    class MapKeyAddDirective(private val key: String) : MapKeyDirective(key) {
        override fun setInContainer(container: MutableMap<String, in Any>, value: Any?) {
            require(value is Number, { "Value to an add directive must be a number (got $value)" })
            val oldValue = container[key]
            if (oldValue == null) {
                container[key] = value
            } else {
                require(oldValue is Number, { "Existing value to an add directive must be a number (got $oldValue)" })
                container[key] = addNumbers(oldValue, value)
            }
        }

        override fun toString(): String {
            return "MapKeyAddDirective(key='$key')"
        }
    }

    /**
     * Modifies a list at the current level, or creates a new list.
     * Setting null removes the item at the specified index.
     */
    open class ListIndexDirective(private val index: Int) :
        DocumentPathDirective<MutableList<in Any>> {
        override fun newContainer(): MutableList<in Any> {
            return CopyOnWriteArrayList()
        }

        override fun getFromContainer(container: MutableList<in Any>): Any? {
            return container[index]
        }

        override fun setInContainer(container: MutableList<in Any>, value: Any?) {
            val concurrentValue = value.asConcurrent()
            container.removeAt(index)
            if (concurrentValue != null) {
                container.add(index, concurrentValue)
            }
        }

        override fun toString(): String {
            return "ListIndexDirective(index=$index)"
        }
    }

    open class ListIndexAddDirective(private val index: Int) : ListIndexDirective(index) {
        override fun setInContainer(container: MutableList<in Any>, value: Any?) {
            require(value is Number, { "Value to an add directive must be a number (got $value)" })
            val oldValue = container[index]
            if (oldValue == null) {
                container[index] = value
            } else {
                require(oldValue is Number, { "Existing value to an add directive must be a number (got $oldValue)" })
                container[index] = addNumbers(oldValue, value)
            }
        }

        override fun toString(): String {
            return "ListIndexAddDirective(index=$index)"
        }
    }

    /**
     * Special list directive for the last index in a list (-1).
     * This directive inserts a new entry if one isn't present already when setting.
     * Setting null deletes the last index (if present).
     */
    open class ListLastIndexDirective : ListIndexDirective(0) {
        override fun getFromContainer(container: MutableList<in Any>): Any? {
            return container.lastOrNull()
        }

        override fun setInContainer(container: MutableList<in Any>, value: Any?) {
            val concurrentValue = value.asConcurrent()
            if (container.isNotEmpty()) {
                container.removeAt(container.lastIndex)
            }
            if (concurrentValue != null) {
                container.add(concurrentValue)
            }
        }

        override fun toString(): String {
            return "ListLastIndexDirective()"
        }
    }

    class ListLastIndexAddDirective : ListLastIndexDirective() {
        override fun setInContainer(container: MutableList<in Any>, value: Any?) {
            require(value is Number, { "Value to an add directive must be a number (got $value)" })
            if (container.isEmpty()) {
                container.add(value)
            } else {
                val index = container.size - 1
                val oldValue = container[index]
                if (oldValue == null) {
                    container[index] = value
                } else {
                    require(
                        oldValue is Number,
                        {
                            "Existing value to an add directive must be a number (got $oldValue)"
                        }
                    )
                    container[index] = addNumbers(oldValue, value)
                }
            }
        }

        override fun toString(): String {
            return "ListLastIndexAddDirective()"
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
            val concurrentValue = value.asConcurrent()
            requireNotNull(concurrentValue) { "Cannot use null for last path insert value" }
            container.add(concurrentValue)
        }

        override fun toString(): String {
            return "ListInsertDirective()"
        }
    }
}
