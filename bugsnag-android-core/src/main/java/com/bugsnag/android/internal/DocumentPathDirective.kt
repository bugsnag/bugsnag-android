package com.bugsnag.android.internal

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

    /**
     * Modifies a particular key in a map, or creates a new (String, Object) map.
     * Setting null removes the value.
     */
    class MapKeyDirective(private val key: String) : DocumentPathDirective<MutableMap<String, in Any>> {
        override fun newContainer(): MutableMap<String, in Any> {
            return mutableMapOf()
        }

        override fun getFromContainer(container: MutableMap<String, in Any>): Any? {
            return container[key]
        }

        override fun setInContainer(container: MutableMap<String, in Any>, value: Any?) {
            if (value == null) {
                container.remove(key)
            } else {
                container[key] = value
            }
        }
    }

    /**
     * Modifies a list at the current level, or creates a new list.
     * Setting null removes the item at the specified index.
     */
    open class ListIndexDirective(private val index: Int) : DocumentPathDirective<MutableList<in Any>> {
        override fun newContainer(): MutableList<in Any> {
            return mutableListOf()
        }

        override fun getFromContainer(container: MutableList<in Any>): Any? {
            return container[index]
        }

        override fun setInContainer(container: MutableList<in Any>, value: Any?) {
            container.removeAt(index)
            if (value != null) {
                container.add(index, value)
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
            if (container.isNotEmpty()) {
                container.removeAt(container.lastIndex)
            }
            if (value != null) {
                container.add(value)
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
            requireNotNull(value) { "Cannot use null for last path insert value" }
            container.add(value)
        }
    }
}
