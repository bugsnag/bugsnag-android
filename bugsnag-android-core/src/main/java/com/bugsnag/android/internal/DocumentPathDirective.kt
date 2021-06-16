package com.bugsnag.android

/**
 * A directive determines how document path commands are interpreted. Making a container,
 * getting from a container, or setting to a container will require different operations to
 * complete, depending on the context.
 */
interface DocumentPathDirective {
    /**
     * Make a new container at the current level.
     *
     * @return The new container.
     */
    fun newContainer(): Any

    /**
     * Get an object from the container at the current level.
     *
     * @param container The container to get the object from.
     * @return The resulting object, or null if not found.
     */
    fun getFromContainer(container: Any): Any?

    /**
     * Set an object in a container at the current level.
     *
     * @param container The container to set a value in.
     * @param value The value to set (can be null, which usually means "remove")
     */
    fun setInContainer(container: Any, value: Any?)

    /**
     * Modifies a particular key in a map, or creates a new (String, Object) map.
     * Setting null removes the value.
     */
    class MapKeyDirective(private val key: String) : DocumentPathDirective {
        override fun newContainer(): Any {
            return mutableMapOf<String, Any>()
        }

        override fun getFromContainer(container: Any): Any? {
            return (container as Map<*, *>)[key]
        }

        override fun setInContainer(container: Any, value: Any?) {
            @Suppress("UNCHECKED_CAST")
            val asMap = container as MutableMap<String, Any>
            if (value == null) {
                asMap.remove(
                    key
                )
            } else {
                asMap[key] = value
            }
        }
    }

    /**
     * Modifies a list at the current level, or creates a new list.
     * Setting null removes the item at the specified index.
     */
    open class ListIndexDirective(private val index: Int) : DocumentPathDirective {
        override fun newContainer(): Any {
            return mutableListOf<Any>()
        }

        override fun getFromContainer(container: Any): Any? {
            return (container as List<Any?>)[index]
        }

        override fun setInContainer(container: Any, value: Any?) {
            @Suppress("UNCHECKED_CAST")
            val asList = container as MutableList<Any>
            asList.removeAt(index)
            if (value != null) {
                asList.add(index, value)
            }
        }
    }

    /**
     * Special list directive for the last index in a list (-1).
     * This directive inserts a new entry if one isn't present already when setting.
     * Setting null deletes the last index (if present).
     */
    class ListLastIndexDirective : ListIndexDirective(0) {
        override fun getFromContainer(container: Any): Any? {
            val containerImpl = container as List<*>
            return if (containerImpl.isEmpty()) {
                null
            } else containerImpl[containerImpl.size - 1]
        }

        override fun setInContainer(container: Any, value: Any?) {
            @Suppress("UNCHECKED_CAST")
            val asList = container as MutableList<Any>
            if (asList.isNotEmpty()) {
                asList.removeAt(asList.size - 1)
            }
            if (value != null) {
                asList.add(value)
            }
        }
    }

    /**
     * Special list directive for insert (path ends in .)
     * This one always inserts a new entry on set, and always returns null on get.
     * Setting null is an error.
     */
    class ListInsertDirective : ListIndexDirective(0) {
        override fun getFromContainer(container: Any): Any? {
            return null
        }

        override fun setInContainer(container: Any, value: Any?) {
            @Suppress("UNCHECKED_CAST")
            val asList = container as MutableList<Any>
            requireNotNull(value) { "Cannot use null for last path insert value" }
            asList.add(value)
        }
    }
}
