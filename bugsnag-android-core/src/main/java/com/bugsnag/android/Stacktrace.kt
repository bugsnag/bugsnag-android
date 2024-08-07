package com.bugsnag.android

import java.io.IOException
import kotlin.math.min

/**
 * Serialize an exception stacktrace and mark frames as "in-project"
 * where appropriate.
 */
internal class Stacktrace : JsonStream.Streamable {

    companion object {
        private const val STACKTRACE_TRIM_LENGTH = 200

        /**
         * Calculates whether a stackframe is 'in project' or not by checking its class against
         * [Configuration.getProjectPackages].
         *
         * For example if the projectPackages included 'com.example', then
         * the `com.example.Foo` class would be considered in project, but `org.example.Bar` would
         * not.
         */
        fun inProject(className: String, projectPackages: Collection<String>): Boolean? {
            return when {
                projectPackages.any { className.startsWith(it) } -> true
                else -> null
            }
        }

        fun serializeStackframe(
            el: StackTraceElement,
            projectPackages: Collection<String>,
            logger: Logger
        ): Stackframe? {
            try {
                val className = el.className
                val methodName = when {
                    className.isNotEmpty() -> className + "." + el.methodName
                    else -> el.methodName
                }

                return Stackframe(
                    methodName,
                    el.fileName ?: "Unknown",
                    el.lineNumber,
                    inProject(className, projectPackages)
                )
            } catch (lineEx: Exception) {
                logger.w("Failed to serialize stacktrace", lineEx)
                return null
            }
        }
    }

    val trace: MutableList<Stackframe>

    constructor(frames: MutableList<Stackframe>) {
        trace = limitTraceLength(frames)
    }

    constructor(
        stacktrace: Array<StackTraceElement>,
        projectPackages: Collection<String>,
        logger: Logger
    ) {
        // avoid allocating new subLists or Arrays by only copying the required number of frames
        // mapping them to our internal Stackframes as we go, roughly equivalent to
        // stacktrace.take(STACKTRACE_TRIM_LENGTH).mapNotNullTo(ArrayList()) { ... }
        val frameCount = min(STACKTRACE_TRIM_LENGTH, stacktrace.size)
        trace = ArrayList(frameCount)
        for (i in 0 until frameCount) {
            val frame = serializeStackframe(stacktrace[i], projectPackages, logger)
            if (frame != null) {
                trace.add(frame)
            }
        }
    }

    private fun limitTraceLength(frames: MutableList<Stackframe>): MutableList<Stackframe> {
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
}
