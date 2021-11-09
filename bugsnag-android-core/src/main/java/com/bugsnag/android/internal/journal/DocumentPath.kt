package com.bugsnag.android.internal.journal

/**
 * DocumentPath records a path into a hierarchical acyclic document, and can be used to modify
 * documents by navigating down the DAG and then applying a modification at that level.
 *
 * Paths follow a dotted notation similar to internet hostnames, such that each path component
 * is separated by a dot '.' If a literal dot is required, use an escape sequence: A backslash
 * character causes the parser to NOT treat the next character specially (regardless of whether
 * it's a special character or not):
 *
 * \. = literal dot
 * \\ = literal backslash
 * \+ = literal plus
 * \z = literal z
 *
 * Path components are either integers or non-integers. Integers refer to the previous path
 * component as a list, providing the index to look up. Non-integers refer to the previous path
 * component as a map, providing the key to look up.
 *
 * For example: the path "foo.bar.3" refers to document root (always a map) with map key "foo",
 * drilled down to map key "bar", drilled down to list index 3. i.e.
 * {
 *     "foo": {
 *         "bar": [
 *             "we expect something to be here (index 0)",
 *             "we expect something to be here (index 1)",
 *             "we expect something to be here (index 2)",
 *             "the thing we're actually interested in (index 3)"
 *         ]
 *     }
 * }
 *
 * Any nonexistent parts of the document are created on-demand as they are encountered while
 * resolving paths (either as a list in the case of integer path components, or as a map for
 * non-integers). In the case of lists, only index 0 of a nonexistent list is valid (in which
 * case the list will be created, and the value stored at index 0).
 *
 * As well, the following special rules apply:
 *
 * - An empty path component refers to the document root, and causes the value to replace
 * the entire document.
 * - The integer path component -1 refers to the last index in the array, or index 0 if the
 * array is empty or nonexistent.
 * - If a path ends in an unescaped dot (e.g. events.exceptions.stacktrace.), it means that the
 * last path entry (e.g. stacktrace) is to be treated as a list, and the value is to be appended
 * to that list.
 * - If a path ends in an unescaped plus (e.g. session.events.handled+), it means that the value
 * must be ADDED to any existing value (or inserted if no value exists yet). Value must be numeric.
 *
 * The document to be modified must be serializable into JSON. The following types are supported:
 * - null
 * - boolean
 * - integer (max 15 digits)
 * - float
 * - string
 * - List(Object)
 * - Map(String, Object)
 */
class DocumentPath(path: String) {
    private val directives: List<DocumentPathDirective<Any>> = toPathDirectives(path)

    /**
     * Modify a document, setting this path in the document to the specified value.
     *
     * @param document The document to modify.
     * @param value    The value to modify with.
     * @return The modified document (may not be the same document that was passed in).
     */
    fun modifyDocument(
        document: MutableMap<in String, out Any>,
        value: Any?
    ): MutableMap<String, Any> {
        if (directives.isEmpty()) {
            require(value is MutableMap<*, *>) { "Value replacing document must be a map" }
            @Suppress("UNCHECKED_CAST")
            return value as MutableMap<String, Any>
        }
        val filledInDocument = fillInMissingContainers(document, 0)
        require(filledInDocument is MutableMap<*, *>) { "Document path must result in a top level map" }
        @Suppress("UNCHECKED_CAST")
        val updatedDocument = filledInDocument as MutableMap<String, Any>

        applyValueToDocument(value, updatedDocument)
        return updatedDocument
    }

    /**
     * Fill in the document parts that will be navigated, but don't exist yet.
     *
     * For example, the path "foo.bar.1.-1" requires a document consisting of:
     * - Map with key "foo" containing:
     * - Map with key "bar" containing:
     * - List with index 1 present, containing:
     * - List where the last index is what we want to modify.
     *
     * Before we can modify the last index, we need to create all of the parent
     * containers (if they don't exist yet). This function recursively descends
     * the document, creating all necessary containers to make this path navigatable.
     *
     * @param parent The parent container (which might be null if it doesn't exist yet)
     * @param index The index of the directive to use if a new container is necessary.
     * @return The container that should be in place of the parent passed in (a list or a map).
     */
    private fun fillInMissingContainers(parent: Any?, index: Int): Any {
        val directive = directives[index]
        val updatedParent = parent ?: directive.newContainer()

        if (index + 1 < directives.size) {
            val result =
                fillInMissingContainers(directive.getFromContainer(updatedParent), index + 1)
            directive.setInContainer(updatedParent, result)
        }
        return updatedParent
    }

    /**
     * Navigate down into a document's sub-containers, then apply the specified value.
     *
     * @param value The value to apply (null means delete)
     * @param document the document to apply this value to.
     */
    private fun applyValueToDocument(value: Any?, document: MutableMap<String, Any>) {
        // Directives 0 through n-1 navigate to where we want to edit the document.
        var currentContainer: Any = document
        for (i in 0 until directives.size - 1) {
            val directive = directives[i]
            currentContainer = directive.getFromContainer(currentContainer)!!
        }

        // Only the last directive does the actual setting.
        val directive = directives.last()
        directive.setInContainer(currentContainer, value)
    }

    override fun toString(): String {
        return "DocumentPath(directives=$directives)"
    }

    companion object {

        private const val ESCAPE_CHAR = '\\'
        private const val PATH_SEPARATOR = '.'
        private const val ADD_OPERATOR = '+'

        // optimization: memoize the results for the path directive
        @JvmField
        internal val memo = HashMap<String, List<DocumentPathDirective<Any>>>()

        private fun containsEscapeSequence(str: String): Boolean {
            for (c in str) {
                if (c == ESCAPE_CHAR) {
                    return true
                }
            }
            return false
        }

        private fun unescape(str: String): String {
            if (!containsEscapeSequence(str)) {
                return str
            }

            val buff = StringBuilder(str.length)
            var escapeIsInitiated = false
            for (c in str) {
                if (escapeIsInitiated) {
                    buff.append(c)
                    escapeIsInitiated = false
                } else if (c == ESCAPE_CHAR) {
                    escapeIsInitiated = true
                } else {
                    buff.append(c)
                }
            }

            require(!escapeIsInitiated) { "Path cannot end on an escape char" }

            return buff.toString()
        }

        /**
         * Generate path directives from a path string.
         * The path cannot have empty components (e.g. "a..b" is invalid).
         */
        internal fun toPathDirectives(path: String): List<DocumentPathDirective<Any>> {
            return memo.getOrPut(path) { toPathDirectivesImpl(path) }
        }

        private fun toPathDirectivesImpl(path: String): List<DocumentPathDirective<Any>> {
            if (path.isEmpty()) {
                return emptyList()
            }

            // Choose an initial capacity that is unlikely to be exceeded.
            // Currently the deepest we go is events.0.exceptions.0.stackTrace.0.xyz
            val optimalArrayLength = 8
            val directives = ArrayList<DocumentPathDirective<*>>(optimalArrayLength)
            val buff = StringBuilder(path.length)
            var escapeIsInitiated = false

            for (c in path) {
                if (escapeIsInitiated) {
                    if (c != PATH_SEPARATOR) {
                        buff.append(ESCAPE_CHAR)
                    }
                    buff.append(c)
                    escapeIsInitiated = false
                } else {
                    when (c) {
                        ESCAPE_CHAR -> escapeIsInitiated = true
                        PATH_SEPARATOR -> {
                            require(!buff.isEmpty()) { "Path component cannot be empty" }
                            directives += toPathDirective(buff.toString())
                            buff.delete(0, buff.length)
                        }
                        else -> buff.append(c)
                    }
                }
            }

            require(!escapeIsInitiated) { "Path cannot end on an escape character" }
            val isLastCharEscaped = (path.length > 1 && path[path.lastIndex - 1] == ESCAPE_CHAR)
            directives.add(toLastPathDirective(buff.toString(), isLastCharEscaped))

            @Suppress("UNCHECKED_CAST")
            return directives as MutableList<DocumentPathDirective<Any>>
        }

        internal fun toPathDirective(pathComponent: String): DocumentPathDirective<out Any> {
            val index = pathComponent.toIntOrNull()
            if (index != null) {
                require(index >= -1) { "List path index $index is invalid" }
                return if (index == -1) {
                    DocumentPathDirective.ListLastIndexDirective()
                } else {
                    DocumentPathDirective.ListIndexDirective(index)
                }
            }

            return DocumentPathDirective.MapKeyDirective(unescape(pathComponent))
        }

        internal fun toLastPathDirective(
            pathComponent: String,
            isLastCharEscaped: Boolean
        ): DocumentPathDirective<out Any> {
            if (pathComponent.isEmpty()) {
                // The original path ended in a dot
                return DocumentPathDirective.ListInsertDirective()
            }

            if (!isLastCharEscaped && pathComponent.last() == ADD_OPERATOR) {
                val subComponent = pathComponent.substring(0, pathComponent.lastIndex)
                require(!subComponent.isEmpty()) { "Path subcomponent cannot be empty" }

                val index = subComponent.toIntOrNull()
                if (index != null) {
                    require(index >= -1) { "List path index $index is invalid" }
                    return if (index == -1) {
                        DocumentPathDirective.ListLastIndexAddDirective()
                    } else {
                        DocumentPathDirective.ListIndexAddDirective(index)
                    }
                }
                return DocumentPathDirective.MapKeyAddDirective(unescape(subComponent))
            }

            return toPathDirective(pathComponent)
        }
    }
}
