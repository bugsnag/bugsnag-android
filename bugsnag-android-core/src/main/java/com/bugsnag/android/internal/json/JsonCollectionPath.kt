package com.bugsnag.android.internal.json

/**
 * A simple path implementation similar to json path, but much simpler. The notation is strictly
 * dot (`'.'`) separated and does not support name escaping. Paths are parsed from strings and
 * can be efficiently evaluated any number of times once parsed, and are thread safe.
 *
 * Paths are in the form `"property.0.-1.*.value"` where `'*'` is a wildcard match, `0` is the
 * first item of an array (or a property named `"0"`) and `-1` is the last element in an array (or
 * a property named `"-1"`). Null values or non-existent values are skipped.
 */
internal class JsonCollectionPath private constructor(
    private val root: PathNode,
    private val path: String
) {
    /**
     * Extract all of the selected values from the given JSON object stored in the given `Map`.
     */
    fun extractFrom(json: Map<String, *>): List<Any> {
        val out = ArrayList<Any>()
        root.visit(json, out::add)
        return out
    }

    override fun toString(): String {
        return path
    }

    companion object {
        val IDENTITY_PATH = JsonCollectionPath(PathNode.TerminalNode, "")

        fun fromString(path: String): JsonCollectionPath {
            if (path.isEmpty()) {
                return IDENTITY_PATH
            }

            val segments = path.split('.')
                .reversed() // we build the path backwards

            var node: PathNode = PathNode.TerminalNode
            segments.forEach { segment ->
                node = segment.toPathNode(node)
            }

            return JsonCollectionPath(node, path)
        }

        private fun String.toPathNode(next: PathNode): PathNode {
            if (this == "*") {
                return PathNode.Wildcard(next)
            }

            val index = this.toIntOrNull()
            if (index != null) {
                return if (index < 0) {
                    PathNode.NegativeIndex(index, next)
                } else {
                    PathNode.PositiveIndex(index, next)
                }
            }

            return PathNode.Property(this, next)
        }
    }

    private sealed class PathNode {
        abstract fun visit(element: Any, collector: (Any) -> Unit)

        abstract class NonTerminalPathNode(protected val next: PathNode) : PathNode()

        class Property(val name: String, next: PathNode) : NonTerminalPathNode(next) {
            override fun visit(element: Any, collector: (Any) -> Unit) {
                if (element is Map<*, *>) {
                    element[name]?.let { next.visit(it, collector) }
                }
            }

            override fun toString(): String = name
        }

        class Wildcard(next: PathNode) : NonTerminalPathNode(next) {
            override fun visit(element: Any, collector: (Any) -> Unit) {
                if (element is Iterable<*>) {
                    element.forEach { item ->
                        item?.let { next.visit(it, collector) }
                    }
                } else if (element is Map<*, *>) {
                    element.values.forEach { item ->
                        item?.let { next.visit(it, collector) }
                    }
                }
            }
        }

        abstract class IndexPathNode(
            protected val index: Int,
            next: PathNode
        ) : NonTerminalPathNode(next) {
            protected abstract fun normalisedIndex(list: List<*>): Int

            override fun visit(element: Any, collector: (Any) -> Unit) {
                if (element is List<*>) {
                    val normalised = normalisedIndex(element)
                    element.getOrNull(normalised)?.let { next.visit(it, collector) }
                } else if (element is Map<*, *>) {
                    val value = element[index.toString()]
                    value?.let { next.visit(it, collector) }
                }
            }
        }

        class PositiveIndex(index: Int, next: PathNode) : IndexPathNode(index, next) {
            override fun normalisedIndex(list: List<*>): Int {
                return index
            }
        }

        class NegativeIndex(index: Int, next: PathNode) : IndexPathNode(index, next) {
            override fun normalisedIndex(list: List<*>): Int {
                return list.size + index
            }
        }

        object TerminalNode : PathNode() {
            override fun visit(element: Any, collector: (Any) -> Unit) {
                collector(element)
            }
        }
    }
}
