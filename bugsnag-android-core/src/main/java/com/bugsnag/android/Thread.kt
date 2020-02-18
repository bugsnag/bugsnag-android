package com.bugsnag.android

import java.io.IOException

/**
 * A representation of a thread recorded in an [Event]
 */
class Thread internal constructor(

    /**
     * The unique ID of the thread (from [java.lang.Thread])
     */
    val id: Long,

    /**
     * The name of the thread (from [java.lang.Thread])
     */
    val name: String,

    /**
     * The type of thread based on the originating platform (intended for internal use only)
     */
    val type: Type,

    /**
     * Whether the thread was the thread that caused the event
     */
    val isErrorReportingThread: Boolean,
    stacktrace: Stacktrace
) : JsonStream.Streamable {

    /**
     * Represents the type of thread captured
     */
    enum class Type(internal val desc: String) {

        /**
         * A thread captured from Android's JVM layer
         */
        ANDROID("android"),

        /**
         * A thread captured from JavaScript
         */
        BROWSER_JS("browserjs")
    }

    /**
     * Controls whether we should capture and serialize the state of all threads at the time
     * of an error.
     */
    enum class ThreadSendPolicy {

        /**
         * Threads should be captured for all events.
         */
        ALWAYS,

        /**
         * Threads should be captured for unhandled events only.
         */
        UNHANDLED_ONLY,

        /**
         * Threads should never be captured.
         */
        NEVER;

        internal companion object {
            fun fromString(str: String) = values().find { it.name == str } ?: ALWAYS
        }
    }

    /**
     * A representation of the thread's stacktrace
     */
    var stacktrace: MutableList<Stackframe> = stacktrace.trace.toMutableList()

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("id").value(id)
        writer.name("name").value(name)
        writer.name("type").value(type.desc)

        writer.name("stacktrace")
        writer.beginArray()
        stacktrace.forEach { writer.value(it) }
        writer.endArray()

        if (isErrorReportingThread) {
            writer.name("errorReportingThread").value(true)
        }
        writer.endObject()
    }
}
