package com.bugsnag.android

/**
 * An [Error] represents information extracted from a [Throwable].
 */
class Error @JvmOverloads internal constructor(

    /**
     * The fully-qualified class name of the [Throwable]
     */
    var errorClass: String,

    /**
     * The message string from the [Throwable]
     */
    var errorMessage: String?,

    /**
     * A representation of the stacktrace
     */
    var stacktrace: List<Stackframe>,

    /**
     * The type of error based on the originating platform (intended for internal use only)
     */
    var type: Type = Type.ANDROID
): JsonStream.Streamable {

    /**
     * Represents the type of error captured
     */
    enum class Type(internal val desc: String) {

        /**
         * An error captured from Android's JVM layer
         */
        ANDROID("android"),

        /**
         * An error captured from JavaScript
         */
        BROWSER_JS("browserjs"),

        /**
         * An error captured from Android's C layer
         */
        C("c")
    }

    internal companion object {
        fun createError(exc: Throwable, projectPackages: Collection<String>, logger: Logger): MutableList<Error> {
            val errors = mutableListOf<Error>()

            var currentEx: Throwable? = exc
            while (currentEx != null) {
                val trace = Stacktrace(currentEx.stackTrace, projectPackages, logger)
                errors.add(Error(currentEx.javaClass.name, currentEx.localizedMessage, trace.trace))
                currentEx = currentEx.cause
            }
            return errors
        }
    }

    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("errorClass").value(errorClass)
        writer.name("message").value(errorMessage)
        writer.name("type").value(type.desc)
        writer.name("stacktrace").value(stacktrace)
        writer.endObject()
    }
}
