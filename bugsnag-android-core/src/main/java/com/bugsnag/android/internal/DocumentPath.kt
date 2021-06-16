package com.bugsnag.android

/**
 * DocumentPath records a path into a hierarchical acyclic document, and can be used to modify
 * documents by navigating down the DAG and then applying a modification at that level.
 *
 * Paths are explained as follows in PLAT-6353:
 *
 * Paths follow a dotted notation similar to internet hostnames, such that each path component
 * is separated by a dot '.' If a literal dot is required, the path supports an escaping
 * mechanism using the backslash character:
 *
 * \. = literal dot
 * \\ = literal backslash
 *
 * Path components are either integers or non-integers. Integers refer to the previous path
 * component as a list, providing the index to look up. Non-integers refer to the previous path
 * component as a map, providing the key to look up (for example, in the path "a.b.3", "a" is
 * treated as a map and "b" is treated as a list, so the path refers to document root with map
 * key "a", which is then indexed with map key "b", which is then indexed with list index 3).
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
 * - If a path ends in a dot (e.g. events.exceptions.stacktrace.), It means that the last
 * path entry (e.g. stacktrace) is to be treated as a list, and the value is to be appended
 * to that list.
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
    val directives: List<DocumentPathDirective> = toPathDirectives(path)

    /**
     * Modify a document, setting this path in the document to the specified value.
     *
     * @param document The document to modify.
     * @param value    The value to modify with.
     * @return The modified document (may not be the same document that was passed in).
     */
    fun modifyDocument(document: MutableMap<String, Any>, value: Any?): MutableMap<String, Any> {
        if (directives.isEmpty()) {
            requireNotNull(value) { "Cannot replace entire document with null" }
            require(value is MutableMap<*, *>) { "Value replacing document must be a mutable map" }
            @Suppress("UNCHECKED_CAST")
            return value as MutableMap<String, Any>
        }
        val updatedDocument = fillInMissingContainers(document, 0)!!
        applyValueToContainer(value, updatedDocument)
        @Suppress("UNCHECKED_CAST")
        return updatedDocument as MutableMap<String, Any>
    }

    // Fill in the document parts that will be navigated, but don't exist yet.
    private fun fillInMissingContainers(parent: Any?, index: Int): Any? {
        var updatedParent = parent
        if (index >= directives.size) {
            return null
        }
        val directive = directives[index]
        if (updatedParent == null) {
            updatedParent = directive.newContainer()
        }
        val result = fillInMissingContainers(directive.getFromContainer(updatedParent), index + 1)
        if (result != null) {
            directive.setInContainer(updatedParent, result)
        }
        return updatedParent
    }

    // Navigate down into a container, then set the specified value.
    private fun applyValueToContainer(value: Any?, container: Any) {
        // These directives navigate to where we want to edit the document.
        var updatedContainer = container
        for (i in 0 until directives.size - 1) {
            val directive = directives[i]
            updatedContainer = directive.getFromContainer(updatedContainer)!!
        }

        // The last directive does the actual setting.
        val directive = directives[directives.size - 1]
        directive.setInContainer(updatedContainer, value)
    }

    companion object {
        fun toPathDirectives(path: String): List<DocumentPathDirective> {
            val directives: MutableList<DocumentPathDirective> = ArrayList(0)
            if (path.isEmpty()) {
                return directives
            }
            var requiresEscaping = false
            var strBegin = 0
            var i = 0
            while (i < path.length) {
                when (path[i]) {
                    '\\' -> {
                        requiresEscaping = true
                        i++
                    }
                    '.' -> {
                        val pathComponent: String = if (requiresEscaping) {
                            unescape(path, strBegin, i)
                        } else {
                            path.substring(strBegin, i)
                        }
                        directives.add(makeDirectiveFromPathComponent(pathComponent))
                        strBegin = i + 1
                        requiresEscaping = false
                    }
                    else -> {
                    }
                }
                i++
            }
            if (strBegin < path.length) {
                val pathComponent: String = if (requiresEscaping) {
                    unescape(path, strBegin, path.length)
                } else {
                    path.substring(strBegin)
                }
                directives.add(makeDirectiveFromPathComponent(pathComponent))
            }
            if (isDotLast(path)) {
                directives.add(DocumentPathDirective.ListInsertDirective())
            }
            return directives
        }

        private fun unescape(str: String, startOffset: Int, endOffset: Int): String {
            val buff = StringBuilder(endOffset - startOffset)
            var i = startOffset
            while (i < endOffset) {
                val ch = str[i]
                if (ch == '\\') {
                    i++
                    require(i < endOffset) { "Path cannot end on an escape char '\\'" }
                    buff.append(str[i])
                } else {
                    buff.append(ch)
                }
                i++
            }
            return buff.toString()
        }

        private fun isDotLast(path: String): Boolean {
            if (path[path.length - 1] != '.') {
                return false
            }
            if (path.length == 1) {
                return true
            }
            var isUnescapedDot = true
            for (i in path.length - 2 downTo 0) {
                val ch = path[i]
                isUnescapedDot = if (ch == '\\') {
                    !isUnescapedDot
                } else {
                    break
                }
            }
            return isUnescapedDot
        }

        private fun makeDirectiveFromPathComponent(pathComponent: String?): DocumentPathDirective {
            if (isInteger(pathComponent)) {
                val index = pathComponent!!.toInt()
                return if (index == -1) {
                    DocumentPathDirective.ListLastIndexDirective()
                } else {
                    DocumentPathDirective.ListIndexDirective(index)
                }
            }
            return DocumentPathDirective.MapKeyDirective(pathComponent!!)
        }

        private fun isInteger(str: String?): Boolean {
            if (str!!.isEmpty()) {
                return false
            }
            var startIndex = 0
            if (str[0] == '-') {
                startIndex = 1
            }
            for (i in startIndex until str.length) {
                val ch = str[i].toInt()
                if (ch < '0'.toInt() || ch > '9'.toInt()) {
                    return false
                }
            }
            return true
        }
    }
}
