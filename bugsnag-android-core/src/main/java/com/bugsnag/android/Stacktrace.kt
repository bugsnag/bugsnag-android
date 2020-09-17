package com.bugsnag.android

import java.io.IOException

/**
 * Serialize an exception stacktrace and mark frames as "in-project"
 * where appropriate.
 */
internal class Stacktrace : JsonStream.Streamable {

    companion object {
        private const val STACKTRACE_TRIM_LENGTH = 200

        fun inProject(className: String, projectPackages: Collection<String>): Boolean? {
            for (packageName in projectPackages) {
                if (className.startsWith(packageName)) {
                    return true
                }
            }
            return null
        }
    }

    val trace: List<Stackframe>
    val logger: Logger

    constructor(
        stacktrace: Array<StackTraceElement>,
        projectPackages: Collection<String>,
        logger: Logger
    ) {
        trace = limitTraceLength(stacktrace.mapNotNull { serializeStackframe(it, projectPackages) })
        this.logger = logger
    }

    constructor(frames: List<Stackframe>, logger: Logger) {
        trace = limitTraceLength(frames)
        this.logger = logger
    }

    private fun <T> limitTraceLength(frames: List<T>): List<T> {
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
    ): Stackframe? {
        try {
            val methodName = when {
                el.className.isNotEmpty() -> el.className + "." + el.methodName
                else -> el.methodName
            }

            return Stackframe(
                methodName,
                if (el.fileName == null) "Unknown" else el.fileName,
                el.lineNumber,
                inProject(el.className, projectPackages)
            )
        } catch (lineEx: Exception) {
            logger.w("Failed to serialize stacktrace", lineEx)
            return null
        }
    }
}
