package com.bugsnag.android

import java.io.IOException
import java.util.HashMap

/**
 * Serialize an exception stacktrace and mark frames as "in-project"
 * where appropriate.
 */
internal class Stacktrace : JsonStream.Streamable {

    companion object {
        private const val STACKTRACE_TRIM_LENGTH = 200
    }

    val trace: List<Stackframe>

    constructor(stacktrace: Array<StackTraceElement>, projectPackages: Collection<String>) {
        trace = limitTraceLength(stacktrace
            .mapNotNull { serializeStackframe(it, projectPackages) }
        ).map { mapToStackframe(it) }
    }

    constructor(frames: List<Map<String, Any?>>) {
        trace = limitTraceLength(frames).map { mapToStackframe(it) }
    }

    private fun mapToStackframe(it: Map<String, Any?>) =
        Stackframe(
            it["method"] as String?,
            it["file"] as String?,
            it["lineNumber"] as Number?,
            it["inProject"] as Boolean?,
            it.filterNot { arrayOf("method", "file", "lineNumber", "inProject").contains(it.key) }
        )

    private fun limitTraceLength(frames: List<Map<String, Any?>>): List<Map<String, Any?>> {
        return when {
            frames.size >= STACKTRACE_TRIM_LENGTH -> frames.subList(0, STACKTRACE_TRIM_LENGTH)
            else -> frames
        }
    }

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginArray()
        trace.forEach { writer.value(it) }
        writer.endArray()
    }

    private fun serializeStackframe(
        el: StackTraceElement,
        projectPackages: Collection<String>
    ): Map<String, Any?>? {
        val map = HashMap<String, Any?>()
        try {
            val methodName = when {
                el.className.isNotEmpty() -> el.className + "." + el.methodName
                else -> el.methodName
            }
            map["method"] = methodName
            map["file"] = if (el.fileName == null) "Unknown" else el.fileName
            map["lineNumber"] = el.lineNumber

            if (inProject(el.className, projectPackages)) {
                map["inProject"] = true
            }
            return map
        } catch (lineEx: Exception) {
            return null
        }
    }

    private fun inProject(className: String, projectPackages: Collection<String>): Boolean {
        for (packageName in projectPackages) {
            if (className.startsWith(packageName)) {
                return true
            }
        }
        return false
    }
}
